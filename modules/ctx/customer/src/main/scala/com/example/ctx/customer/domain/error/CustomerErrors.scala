package com.example.ctx.customer.domain.error

import com.example.ctx.customer.domain.error.CustomerErrorReason._
import com.example.lib.common.domain.error.{AppFailure, ErrorCategory, HttpBadRequest, HttpError, HttpNotFound}
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}

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

final case class InvalidAddressLineError(message: String)
    extends CustomerError(errorReason = InvalidAddressLine, message = message, cause = None)
    with HttpBadRequest

final case class InvalidCityError(message: String)
    extends CustomerError(errorReason = InvalidCity, message = message, cause = None)
    with HttpBadRequest

final case class InvalidPostalCodeError(message: String)
    extends CustomerError(errorReason = InvalidPostalCode, message = message, cause = None)
    with HttpBadRequest

final case class AddressNotFoundError private (message: String, cause: Option[Throwable])
    extends CustomerError(errorReason = AddressNotFound, message = message, cause = cause)
    with HttpNotFound

object AddressNotFoundError {
  def withId(id: AddressId): AddressNotFoundError =
    AddressNotFoundError(
      message = s"Address with id=${AddressId.unwrap(id)} is not found",
      cause = None
    )
}
