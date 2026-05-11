package com.example.it.http

import com.example.app.server.ServerRoutes
import com.example.common.domain.service.Transactor
import com.example.common.impl.config.{OtelConfig, OtelTracing}
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.impl.telemetry.AppTracing
import com.example.common.test.TestDb
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.CustomerApiDirectImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import com.example.http.HealthRoutes
import com.example.notification.app.NotificationAppServiceImpl
import com.example.notification.impl.http.NotificationRoutes
import com.example.notification.impl.service.NotificationServiceImpl
import com.example.notification.impl.service.repo.NotificationRepoMySQLImpl
import zio._
import zio.http._
import zio.schema.Schema
import zio.schema.codec.{JsonCodec => SchemaJsonCodec}
import zio.telemetry.opentelemetry.tracing.Tracing

/** End-to-end test harness: a real zio-http server bound to an ephemeral port, fed by `TestDb.freshSchemaLayer` so each
  * test gets an isolated MySQL schema underneath.
  *
  * The composition rebuilds the production layer stack from `CustomerRepoMySQLImpl` upward and ends in
  * `ServerRoutes.layer` — the same composition `ServerApp` runs in production. Two substitutions:
  *   - `DataSource` / `SqlContext` / `Transactor` come from `TestDb.freshSchemaLayer` (test schema, dropped on close)
  *   - `Server.Config` is hardcoded to ephemeral port (`0`); the actual bound port is read back via `Server.port`
  *   - `OtelConfig` is supplied directly with tracing disabled — `AppTracing.live` then builds a no-op `Tracing`
  *
  * Tests provide `TestServer.layer` to a spec, then issue requests via `testServer.get("/customers/...")`. The
  * underlying `Client` and `Server` are shut down when the scope closes.
  */
final case class TestServer(baseUrl: String) {

  def get(path: String): ZIO[Client, Throwable, Response] =
    for {
      url <- ZIO.fromEither(URL.decode(s"$baseUrl$path"))
      res <- ZIO.serviceWithZIO[Client](_.batched(Request.get(url)))
    } yield res

  /** Issue a GET, decode the JSON body into `A`, return status + parsed body. Decoding failures surface as `Throwable`
    * — tests asserting on a 404 still expect the body to parse as `ErrorTO`. The decoder is derived from the value's
    * `Schema` via zio-schema-json — TOs carry only `Schema` instances, the zio-json codec materializes here.
    */
  def getJson[A](path: String)(implicit schema: Schema[A]): ZIO[Client, Throwable, JsonResponse[A]] = {
    val decoder = SchemaJsonCodec.jsonCodec(schema).decoder
    for {
      response <- get(path)
      body     <- response.body.asString
      parsed <- ZIO
        .fromEither(decoder.decodeJson(body))
        .mapError(msg => new RuntimeException(s"Failed to parse response body: $msg — body was: $body"))
    } yield JsonResponse(status = response.status, body = parsed)
  }

  /** Issue a POST with a typed JSON body, decode the response into `Resp`. Mirrors `getJson` — both the request and the
    * response go through `Schema`-derived codecs, so the call site stays free of zio-json plumbing.
    */
  def postJson[Req, Resp](path: String, body: Req)(implicit
      reqSchema: Schema[Req],
      respSchema: Schema[Resp]
  ): ZIO[Client, Throwable, JsonResponse[Resp]] = {
    val reqEncoder  = SchemaJsonCodec.jsonCodec(reqSchema).encoder
    val respDecoder = SchemaJsonCodec.jsonCodec(respSchema).decoder
    val payload     = reqEncoder.encodeJson(body, indent = None).toString
    for {
      url <- ZIO.fromEither(URL.decode(s"$baseUrl$path"))
      request = Request
        .post(url, Body.fromString(payload))
        .addHeader(Header.ContentType(MediaType.application.json))
      response <- ZIO.serviceWithZIO[Client](_.batched(request))
      bodyStr  <- response.body.asString
      parsed <- ZIO
        .fromEither(respDecoder.decodeJson(bodyStr))
        .mapError(msg => new RuntimeException(s"Failed to parse response body: $msg — body was: $bodyStr"))
    } yield JsonResponse(status = response.status, body = parsed)
  }
}

/** Status + parsed body for `TestServer.getJson` / `postJson`. */
final case class JsonResponse[A](status: Status, body: A)

object TestServer {

  private val ephemeralServerConfig: ULayer[Server.Config] =
    ZLayer.succeed(Server.Config.default.binding("localhost", 0))

  private val serverLayer: ZLayer[Any, Throwable, Server] =
    ephemeralServerConfig >>> Server.live

  private val testOtelConfig: ULayer[OtelConfig] =
    ZLayer.succeed(OtelConfig(serviceName = "scala-zio-example-test", tracing = OtelTracing.Disabled))

  private val backendLayer: ZLayer[Any, Throwable, SqlContext & Transactor & ServerRoutes & Tracing] =
    ZLayer.make[SqlContext & Transactor & ServerRoutes & Tracing](
      // ── persistence (test substitution: fresh schema per spec) ──
      TestDb.freshSchemaLayer,
      // ── tracing (no-op, configured Disabled) ──
      testOtelConfig,
      AppTracing.live,
      // ── customer ctx ──
      CustomerRepoMySQLImpl.layer,
      AddressRepoMySQLImpl.layer,
      CustomerServiceImpl.layer,
      AddressServiceImpl.layer,
      CustomerAppServiceImpl.layer,
      CustomerApiDirectImpl.layer,
      CustomerRoutes.layer,
      // ── notification ctx ──
      NotificationRepoMySQLImpl.layer,
      NotificationServiceImpl.layer,
      NotificationAppServiceImpl.layer,
      NotificationRoutes.layer,
      // ── operational ──
      HealthRoutes.layer,
      // ── route composition ──
      ServerRoutes.layer
    )

  private val testServerOnly: ZLayer[ServerRoutes & Server & Tracing, Throwable, TestServer] =
    ZLayer.scoped {
      for {
        sr   <- ZIO.service[ServerRoutes]
        srv  <- ZIO.service[Server]
        _    <- srv.install(sr.all)
        port <- srv.port
        baseUrl = s"http://localhost:$port"
      } yield TestServer(baseUrl = baseUrl)
    }

  /** Full e2e environment: backend stack + running server + http client + the `TestServer` accessor. Logger setup lives
    * at the spec level via `TestLogger` in `IntegrationSpec.bootstrap` — ZLayer can't reach the runtime's logger set.
    */
  val layer: ZLayer[Any, Throwable, TestServer & Client & SqlContext & Transactor] =
    (backendLayer ++ serverLayer) >+> testServerOnly >+> Client.default
}
