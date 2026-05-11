package com.example.it.http

import com.example.app.server.ServerRoutes
import com.example.common.domain.service.Transactor
import com.example.common.impl.config.{OtelConfig, OtelTracing}
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.impl.telemetry.AppTracing
import com.example.common.test.TestDb
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import com.example.http.HealthRoutes
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
}

/** Result of `TestServer.getJson` — case class instead of a tuple to avoid Scala 2 for-comp destructuring friction. */
final case class JsonResponse[A](status: Status, body: A)

object TestServer {

  private val ephemeralServerConfig: ULayer[Server.Config] =
    ZLayer.succeed(Server.Config.default.binding("localhost", 0))

  private val serverLayer: ZLayer[Any, Throwable, Server] =
    ephemeralServerConfig >>> Server.live

  private val testOtelConfig: ULayer[OtelConfig] =
    ZLayer.succeed(OtelConfig(serviceName = "scala-zio-example-test", tracing = OtelTracing.Disabled))

  private val backendLayer: ZLayer[Any, Throwable, SqlContext & Transactor & ServerRoutes & Tracing] =
    TestDb.freshSchemaLayer >+>
      CustomerRepoMySQLImpl.layer >+>
      AddressRepoMySQLImpl.layer >+>
      CustomerServiceImpl.layer >+>
      AddressServiceImpl.layer >+>
      CustomerAppServiceImpl.layer >+>
      CustomerRoutes.layer >+>
      HealthRoutes.layer >+>
      (testOtelConfig >>> AppTracing.live) >+>
      ServerRoutes.layer

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

  /** Full e2e environment: backend stack + running server + http client + the `TestServer` accessor.
    *
    * Logger setup lives at the spec level — see [[com.example.common.test.IntegrationSpec]]. Specs that extend it get
    * [[com.example.common.test.TestLogger]] installed in their `bootstrap`, where ZIO's runtime config (default
    * loggers, etc.) is mutable. A regular `ZLayer` here can't reach the runtime's logger set, so default loggers leak
    * through.
    */
  val layer: ZLayer[Any, Throwable, TestServer & Client & SqlContext & Transactor] =
    (backendLayer ++ serverLayer) >+> testServerOnly >+> Client.default
}
