package com.example.common.domain.error.backend

import com.example.common.domain.error.api.{HttpError, HttpInternalServerError}
import com.example.common.domain.error.backend.BackendErrorReason._
import com.example.common.domain.error.{AppFailure, ErrorCategory}

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
