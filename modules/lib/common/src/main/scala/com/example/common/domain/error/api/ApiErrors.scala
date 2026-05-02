package com.example.common.domain.error.api

import com.example.common.domain.error.api.ApiErrorReason._
import com.example.common.domain.error.{AppFailure, ErrorCategory}

abstract class ApiError(
    errorReason: ApiErrorReason,
    message: String,
    cause: Option[Throwable] = None
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory   = ErrorCategory.Api
  val reason: ApiErrorReason    = errorReason
}

final case class AuthenticationFailedError(message: String = "Authentication failed")
    extends ApiError(AuthenticationFailed, message)
    with HttpUnauthorized

final case class AuthorizationFailedError(message: String = "Authorization failed")
    extends ApiError(AuthorizationFailed, message)
    with HttpForbidden

final case class MalformedRequestContentError(message: String, cause: Option[Throwable] = None)
    extends ApiError(MalformedRequestContent, message, cause)
    with HttpBadRequest

final case class ValidationError(message: String)
    extends ApiError(Validation, message)
    with HttpBadRequest

final case class UrlPathNotFoundError(path: String)
    extends ApiError(UrlPathNotFound, s"No route matches path: $path")
    with HttpNotFound

final case class UnhandledApiError(message: String, cause: Option[Throwable] = None)
    extends ApiError(UnhandledError, message, cause)
    with HttpInternalServerError
