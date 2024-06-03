package org.organization.http

import org.organization.AppEnv.AppEnv
import org.organization.http.swagger.SwaggerApiEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.http._
import zio.{Duration, RIO, ZIO}

object AppServer {

  private val composedMiddlewares: Middleware[AppEnv] =
    Middleware.timeout(Duration.fromSeconds(10))

  private val httpInterpreter: Routes[AppEnv, Response] = {
    val endpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerApiEndpoint.common
    ZioHttpInterpreter().toHttp(endpoints)
  }

  private val app: Routes[AppEnv, Response] =
    httpInterpreter @@ composedMiddlewares

  def serve: RIO[AppEnv, Nothing] =
    Server
      .install(app)
      .flatMap(p => ZIO.logInfo(s"Started server on port $p"))
      .zipRight(ZIO.never)
}
