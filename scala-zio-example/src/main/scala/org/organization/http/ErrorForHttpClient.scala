package org.organization.http

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._

/** Describes errors that we could send in a http response
  *
  * NB: when modifying this type you must remember to update the endpoint output encoder.
  */
sealed trait ErrorForHttpClient
case object NotFound                                extends ErrorForHttpClient
final case class BadRequest(messageForUser: String) extends ErrorForHttpClient
case object Unauthorized                            extends ErrorForHttpClient
case object InternalServerError                     extends ErrorForHttpClient

object ErrorForHttpClient {
  val endpointOutputEncoder: EndpointOutput.OneOf[ErrorForHttpClient, ErrorForHttpClient] =
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
      oneOfDefaultVariant(
        statusCode(StatusCode.InternalServerError)
          .and(emptyOutputAs(InternalServerError).description("Internal Server Error"))
      )
    )

}
