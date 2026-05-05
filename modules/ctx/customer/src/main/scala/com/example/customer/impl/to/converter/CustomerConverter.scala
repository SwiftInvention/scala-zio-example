package com.example.customer.impl.to.converter

import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.to.CustomerTO
import com.example.customer.domain.model.{Customer, CustomerName, Email}

/** TO ↔ domain mapping for Customer.
  *
  * Lives in `customer/impl/to/converter/` — the only place that sees both sides (the domain entity from this module,
  * the TO from `customer-api`).
  *
  * `toCustomer` returns `AppIO[Customer]` because constructing the typed fields (`Email`, `CustomerName`) is effectful
  * — the smart constructors may fail with an `AppFailure`. `toCustomerTO` is pure: extracting `.value` never fails.
  */
object CustomerConverter {

  def toCustomerTO(d: Customer): CustomerTO =
    CustomerTO(id = d.id, email = d.email.value, name = d.name.value)

  def toCustomer(to: CustomerTO): AppIO[Customer] =
    for {
      email <- Email(to.email)
      name  <- CustomerName(to.name)
    } yield Customer(id = to.id, email = email, name = name)
}
