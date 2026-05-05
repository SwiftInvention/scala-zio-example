package com.example.customer.domain.error

import com.example.common.domain.error.api.{HttpBadRequest, HttpError, HttpNotFound}
import com.example.common.domain.error.{AppFailure, ErrorCategory}
import com.example.common.domain.model.NewTypes.CustomerId
import com.example.customer.domain.error.CustomerErrorReason._

abstract class CustomerError(
    errorReason: CustomerErrorReason,
    message: String,
    cause: Option[Throwable]
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory     = ErrorCategory.Customer
  val reason: CustomerErrorReason = errorReason
}

final case class CustomerNotFoundError private (message: String, cause: Option[Throwable])
    extends CustomerError(errorReason = NotFound, message = message, cause = cause)
    with HttpNotFound

object CustomerNotFoundError {
  def withId(id: CustomerId): CustomerNotFoundError =
    CustomerNotFoundError(
      message = s"Customer with id=${CustomerId.unwrap(id)} is not found",
      cause = None
    )
}

final case class InvalidEmailError(message: String)
    extends CustomerError(errorReason = InvalidEmail, message = message, cause = None)
    with HttpBadRequest

final case class InvalidCustomerNameError(message: String)
    extends CustomerError(errorReason = InvalidName, message = message, cause = None)
    with HttpBadRequest
