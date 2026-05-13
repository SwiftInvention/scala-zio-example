package com.example.common.domain.error.backend

import com.example.common.domain.error.backend.BackendErrorReason._
import com.example.common.domain.error.{
  AppFailure,
  ErrorCategory,
  HttpError,
  HttpInternalServerError,
  HttpServiceUnavailable
}

abstract class BackendError(
    errorReason: BackendErrorReason,
    message: String,
    cause: Option[Throwable]
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory      = ErrorCategory.Backend
  val reason: BackendErrorReason   = errorReason
  override val description: String = "Internal server error"
}

final case class InternalServerError(message: String, cause: Option[Throwable])
    extends BackendError(errorReason = InternalError, message = message, cause = cause)
    with HttpInternalServerError

final case class DbError(message: String, cause: Option[Throwable])
    extends BackendError(errorReason = Database, message = message, cause = cause)
    with HttpInternalServerError

final case class ConfigError(message: String, cause: Option[Throwable])
    extends BackendError(errorReason = Config, message = message, cause = cause)
    with HttpInternalServerError

final case class ProbeTimeoutError(message: String, cause: Option[Throwable])
    extends BackendError(errorReason = Timeout, message = message, cause = cause)
    with HttpServiceUnavailable
