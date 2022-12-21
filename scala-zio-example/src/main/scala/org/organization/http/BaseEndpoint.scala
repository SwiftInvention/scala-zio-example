package org.organization.http

import io.circe.generic.auto._
import org.organization.AppEnv.AppEnv
import org.organization.http.PersonEndpoint.ZServerEndpoint
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir.RichZEndpoint
import zio.ZIO

object BaseEndpoint {

  private val basePath = "api" / "v1"

  private val baseEndpoint: Endpoint[Unit, Unit, ErrorForHttpClient, Unit, Any] = {
    endpoint
      .in(basePath)
      .errorOut(
        oneOf[ErrorForHttpClient](
          oneOfVariant(
            statusCode(StatusCode.NotFound).and(emptyOutputAs(NotFound).description("Not found"))
          ),
          oneOfVariant(
            statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest].description("Bad request"))
          ),
          oneOfVariant(
            statusCode(StatusCode.Unauthorized)
              .and(emptyOutputAs(Unauthorized).description("Unauthorized"))
          ),
          oneOfVariant(
            statusCode(StatusCode.InternalServerError)
              .and(emptyOutputAs(InternalServerError).description("Internal Server Error"))
          )
        )
      )
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
