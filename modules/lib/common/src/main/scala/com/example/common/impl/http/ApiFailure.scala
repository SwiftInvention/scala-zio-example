package com.example.common.impl.http

import com.example.common.domain.error.{
  AppFailure,
  HttpBadRequest,
  HttpForbidden,
  HttpInternalServerError,
  HttpNotFound,
  HttpServiceUnavailable,
  HttpUnauthorized
}
import zio.http.Status
import zio.http.codec.HttpCodec
import zio.schema.Schema

/** Wire-format error type for the typed-Endpoint API. One variant per HTTP status the codebase's `HttpError` traits
  * declare. Each pins a specific status; the framework picks the right variant's codec at serialization time based on
  * the value the route returns.
  *
  * Each variant wraps an `ErrorTO` via a Schema transform — the JSON body on the wire is identical across statuses
  * (`{"code": ..., "category": ..., "reason": ..., "description": ...}`). The wrapper exists for type discipline at the
  * route boundary, not for the wire format.
  *
  * Endpoints declare which variants they can return via `.outErrors[ApiFailure](badRequestCodec, ...)` — see the codec
  * vals below and the per-endpoint usage in `<Name>Endpoints.scala`. Routes do `mapError(ApiFailure.from)` once at the
  * boundary.
  *
  * Adding a new `HttpError` trait (a new status family) requires four coordinated changes here: a new variant, a new
  * codec val, a new branch in `from` (the build breaks here if you forget), and a new entry in each `<Name>Endpoints`'
  * `outErrors` arg list.
  */
sealed trait ApiFailure {
  def body: ErrorTO
}

object ApiFailure {

  final case class BadRequestResponse(body: ErrorTO) extends ApiFailure
  object BadRequestResponse {
    implicit val schema: Schema[BadRequestResponse] =
      Schema[ErrorTO].transform(BadRequestResponse(_), _.body)
  }

  final case class UnauthorizedResponse(body: ErrorTO) extends ApiFailure
  object UnauthorizedResponse {
    implicit val schema: Schema[UnauthorizedResponse] =
      Schema[ErrorTO].transform(UnauthorizedResponse(_), _.body)
  }

  final case class ForbiddenResponse(body: ErrorTO) extends ApiFailure
  object ForbiddenResponse {
    implicit val schema: Schema[ForbiddenResponse] =
      Schema[ErrorTO].transform(ForbiddenResponse(_), _.body)
  }

  final case class NotFoundResponse(body: ErrorTO) extends ApiFailure
  object NotFoundResponse {
    implicit val schema: Schema[NotFoundResponse] =
      Schema[ErrorTO].transform(NotFoundResponse(_), _.body)
  }

  final case class InternalResponse(body: ErrorTO) extends ApiFailure
  object InternalResponse {
    implicit val schema: Schema[InternalResponse] =
      Schema[ErrorTO].transform(InternalResponse(_), _.body)
  }

  final case class ServiceUnavailableResponse(body: ErrorTO) extends ApiFailure
  object ServiceUnavailableResponse {
    implicit val schema: Schema[ServiceUnavailableResponse] =
      Schema[ErrorTO].transform(ServiceUnavailableResponse(_), _.body)
  }

  /** One codec per variant. Defined once here, imported and listed positionally per endpoint. zio-http's `outErrors` is
    * per-arity-overloaded (not varargs), so a `Seq` + splat doesn't compile — every call site spells out all six.
    */
  val badRequestCodec         = HttpCodec.error[BadRequestResponse](Status.BadRequest)
  val unauthorizedCodec       = HttpCodec.error[UnauthorizedResponse](Status.Unauthorized)
  val forbiddenCodec          = HttpCodec.error[ForbiddenResponse](Status.Forbidden)
  val notFoundCodec           = HttpCodec.error[NotFoundResponse](Status.NotFound)
  val internalCodec           = HttpCodec.error[InternalResponse](Status.InternalServerError)
  val serviceUnavailableCodec = HttpCodec.error[ServiceUnavailableResponse](Status.ServiceUnavailable)

  /** Map an `AppFailure` to its `ApiFailure` variant. Matches on `f.asHttpError`, the self-typed `HttpError` view —
    * exhaustive over `HttpError`'s sealed sub-traits, so the build breaks at compile time if a new `HttpError` trait is
    * added without a matching `ApiFailure` variant + branch here.
    */
  def from(f: AppFailure): ApiFailure = {
    val to = ErrorTO.from(f)
    f.asHttpError match {
      case _: HttpBadRequest          => BadRequestResponse(to)
      case _: HttpUnauthorized        => UnauthorizedResponse(to)
      case _: HttpForbidden           => ForbiddenResponse(to)
      case _: HttpNotFound            => NotFoundResponse(to)
      case _: HttpInternalServerError => InternalResponse(to)
      case _: HttpServiceUnavailable  => ServiceUnavailableResponse(to)
    }
  }
}
