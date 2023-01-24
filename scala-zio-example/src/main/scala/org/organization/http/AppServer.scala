package org.organization.http

import org.organization.AppEnv.AppEnv
import org.organization.http.swagger.SwaggerApiEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import zio.http._
import zio.http.middleware.HttpMiddleware
import zio.{Duration, RIO, ZIO}

object AppServer {

  private val composedMiddlewares: HttpMiddleware[Any, Throwable] =
    Middleware.timeout(Duration.fromSeconds(10))

  private val httpInterpreter =
    ZioHttpInterpreter().toHttp(SwaggerApiEndpoint.common)

  private val app: Http[AppEnv, Throwable, Request, Response] =
    httpInterpreter @@ composedMiddlewares

  def serve: RIO[AppEnv, Nothing] =
    Server.install(app).flatMap(p => ZIO.log(s"Started server on port $p") *> ZIO.never)
}
