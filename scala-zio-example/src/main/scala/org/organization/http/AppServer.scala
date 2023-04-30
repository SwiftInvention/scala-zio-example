package org.organization.http

import org.organization.AppEnv.AppEnv
import org.organization.http.swagger.SwaggerApiEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http._
import zio.{Duration, RIO, ZIO}

object AppServer {

  private val composedMiddlewares: HttpAppMiddleware[AppEnv, AppEnv, Nothing, Throwable] =
    HttpAppMiddleware.timeout(Duration.fromSeconds(10))

  private val httpInterpreter: HttpApp[AppEnv, Throwable] =
    ZioHttpInterpreter().toHttp(SwaggerApiEndpoint.common)

  private val app: HttpApp[AppEnv, Throwable] =
    httpInterpreter @@ composedMiddlewares
  def serve: RIO[AppEnv, Nothing] =
    Server
      .install(app.withDefaultErrorResponse)
      .flatMap(p => ZIO.logInfo(s"Started server on port $p"))
      .zipRight(ZIO.never)
}
