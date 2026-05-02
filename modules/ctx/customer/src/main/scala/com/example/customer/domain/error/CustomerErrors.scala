package com.example.customer.domain.error

import com.example.common.domain.error.api.{HttpError, HttpNotFound}
import com.example.common.domain.error.{AppFailure, ErrorCategory}
import com.example.common.domain.model.NewTypes.CustomerId
import com.example.customer.domain.error.CustomerErrorReason._

abstract class CustomerError(
    errorReason: CustomerErrorReason,
    message: String,
    cause: Option[Throwable] = None
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory      = ErrorCategory.Customer
  val reason: CustomerErrorReason  = errorReason
}

final case class CustomerNotFoundError private (message: String, cause: Option[Throwable] = None)
    extends CustomerError(NotFound, message, cause)
    with HttpNotFound

object CustomerNotFoundError {
  def withId(id: CustomerId): CustomerNotFoundError =
    CustomerNotFoundError(s"Customer with id=${CustomerId.unwrap(id)} is not found")
}
