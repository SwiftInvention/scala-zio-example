package org.organization.http

import org.organization.AppEnv.AppEnv
import org.organization.http.swagger.SwaggerApiEndpoint
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir.ZServerEndpoint
import zio.http._
import zio.{Duration, RIO, ZIO}

import scala.annotation.nowarn

object AppServer {

  private val composedMiddlewares: Middleware[AppEnv] =
    Middleware.timeout(Duration.fromSeconds(10))

  @nowarn("msg=class HttpApp in package http is deprecated")
  private val httpInterpreter: HttpApp[AppEnv] = {
    val endpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerApiEndpoint.common
    ZioHttpInterpreter().toHttp(endpoints)
  }

  @nowarn("msg=class HttpApp in package http is deprecated")
  private val app: HttpApp[AppEnv] =
    httpInterpreter @@ composedMiddlewares

  @nowarn("msg=method install in object Server is deprecated")
  def serve: RIO[AppEnv, Nothing] =
    Server
      .install(app)
      .flatMap(p => ZIO.logInfo(s"Started server on port $p"))
      .zipRight(ZIO.never)
}
