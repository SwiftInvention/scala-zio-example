package com.example.lib.common.domain.error.backend

import com.example.lib.common.domain.error.backend.BackendErrorReason._
import com.example.lib.common.domain.error.{
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

/** Read-path consistency failure: a value that the domain or schema implies must exist is missing from the underlying
  * data. Use where the invariant lives at the storage layer (foreign keys, NOT NULL columns) or in a cross-context
  * contract, but isn't expressed in the PE / repo types the caller is working with. The `message` carries the specific
  * case (which entity, which ids, which relationship).
  */
final case class DataIntegrityError(message: String, cause: Option[Throwable])
    extends BackendError(errorReason = DataIntegrity, message = message, cause = cause)
    with HttpInternalServerError
