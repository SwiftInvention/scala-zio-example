package org.organization.http

import org.organization.AppEnv.AppEnv
import org.organization.http.PersonEndpoint.ZServerEndpoint
import sttp.tapir._
import sttp.tapir.ztapir.RichZEndpoint
import zio.ZIO

object BaseEndpoint {

  private val basePath = "api" / "v1"

  private val baseEndpoint: Endpoint[Unit, Unit, ErrorForHttpClient, Unit, Any] = {
    endpoint
      .in(basePath)
      .errorOut(ErrorForHttpClient.endpointOutputEncoder)
  }

  type HttpIO[T] = ZIO[AppEnv, ErrorForHttpClient, T]

  def makeEndpoint(
      name: String,
      description: String
  ): PublicEndpoint[Unit, ErrorForHttpClient, Unit, Any] = {
    baseEndpoint
      .name(name)
      .description(description)
  }

  def makeEndpointHandler[INPUT, OUTPUT, R](
      endpoint: PublicEndpoint[INPUT, ErrorForHttpClient, OUTPUT, R]
  )(
      logic: INPUT => HttpIO[OUTPUT]
  ): ZServerEndpoint[AppEnv, R] = endpoint.zServerLogic(logic)

}
