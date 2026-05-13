package com.example.common.domain.error.api

import com.example.common.domain.error.api.ApiErrorReason._
import com.example.common.domain.error.{
  AppFailure,
  ErrorCategory,
  HttpBadRequest,
  HttpError,
  HttpForbidden,
  HttpInternalServerError,
  HttpNotFound,
  HttpUnauthorized
}

abstract class ApiError(
    errorReason: ApiErrorReason,
    message: String,
    cause: Option[Throwable]
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory = ErrorCategory.Api
  val reason: ApiErrorReason  = errorReason
}

final case class AuthenticationFailedError(message: String)
    extends ApiError(errorReason = AuthenticationFailed, message = message, cause = None)
    with HttpUnauthorized

final case class AuthorizationFailedError(message: String)
    extends ApiError(errorReason = AuthorizationFailed, message = message, cause = None)
    with HttpForbidden

final case class MalformedRequestContentError(message: String, cause: Option[Throwable])
    extends ApiError(errorReason = MalformedRequestContent, message = message, cause = cause)
    with HttpBadRequest

final case class ValidationError(message: String)
    extends ApiError(errorReason = Validation, message = message, cause = None)
    with HttpBadRequest

final case class UrlPathNotFoundError(path: String)
    extends ApiError(errorReason = UrlPathNotFound, message = s"No route matches path: $path", cause = None)
    with HttpNotFound

final case class UnhandledApiError(message: String, cause: Option[Throwable])
    extends ApiError(errorReason = UnhandledError, message = message, cause = cause)
    with HttpInternalServerError
