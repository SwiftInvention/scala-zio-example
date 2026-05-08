package com.example.it.http

import com.example.common.domain.service.Transactor
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.TestDb
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import zio._
import zio.http._

/** End-to-end test harness: a real zio-http server bound to an ephemeral port, fed by `TestDb.freshSchemaLayer` so each
  * test gets an isolated MySQL schema underneath.
  *
  * The layer composition rebuilds the production layer stack from `CustomerRepoMySQLImpl` upward, substituting two
  * pieces:
  *   - `DataSource` / `SqlContext` / `Transactor` come from `TestDb.freshSchemaLayer` (test schema, dropped on close)
  *   - `Server.Config` is hardcoded to ephemeral port (`0`); the actual bound port is read back via `Server.port`
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
    * — tests asserting on a 404 still expect the body to parse as `ErrorTO`.
    */
  def getJson[A: zio.json.JsonDecoder](path: String): ZIO[Client, Throwable, JsonResponse[A]] =
    for {
      response <- get(path)
      body     <- response.body.asString
      parsed <- ZIO
        .fromEither(zio.json.JsonDecoder[A].decodeJson(body))
        .mapError(msg => new RuntimeException(s"Failed to parse response body: $msg — body was: $body"))
    } yield JsonResponse(status = response.status, body = parsed)
}

/** Result of `TestServer.getJson` — case class instead of a tuple to avoid Scala 2 for-comp destructuring friction. */
final case class JsonResponse[A](status: Status, body: A)

object TestServer {

  private val ephemeralServerConfig: ULayer[Server.Config] =
    ZLayer.succeed(Server.Config.default.binding("localhost", 0))

  private val serverLayer: ZLayer[Any, Throwable, Server] =
    ephemeralServerConfig >>> Server.live

  private val backendLayer: ZLayer[Any, Throwable, SqlContext & Transactor & CustomerRoutes] =
    TestDb.freshSchemaLayer >+>
      CustomerRepoMySQLImpl.layer >+>
      AddressRepoMySQLImpl.layer >+>
      CustomerServiceImpl.layer >+>
      AddressServiceImpl.layer >+>
      CustomerAppServiceImpl.layer >+>
      CustomerRoutes.layer

  private val testServerOnly: ZLayer[CustomerRoutes & Server, Throwable, TestServer] =
    ZLayer.scoped {
      for {
        routes <- ZIO.service[CustomerRoutes]
        srv    <- ZIO.service[Server]
        _      <- srv.install(routes.routes)
        port   <- srv.port
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
