package com.example.lib.common.domain.error.domain

import com.example.lib.common.domain.error.domain.DomainErrorReason._
import com.example.lib.common.domain.error.{AppFailure, ErrorCategory, HttpBadRequest, HttpError}

/** Errors raised by smart-constructor value objects that live in `lib/common` (`Email`, `CustomerName`, …). Category
  * `Domain` because the failure is "value didn't satisfy the shared domain invariant," which is independent of any one
  * bounded context. Maps to 400 — these surface at boundaries where caller-supplied data is being lifted into a typed
  * value.
  */
abstract class DomainError(
    errorReason: DomainErrorReason,
    message: String,
    cause: Option[Throwable]
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory   = ErrorCategory.Domain
  val reason: DomainErrorReason = errorReason
}

final case class InvalidEmailError(message: String)
    extends DomainError(errorReason = InvalidEmail, message = message, cause = None)
    with HttpBadRequest

final case class InvalidCustomerNameError(message: String)
    extends DomainError(errorReason = InvalidCustomerName, message = message, cause = None)
    with HttpBadRequest

final case class InvalidURLError(message: String)
    extends DomainError(errorReason = InvalidURL, message = message, cause = None)
    with HttpBadRequest
