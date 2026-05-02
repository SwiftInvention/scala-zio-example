package com.example.customer.impl.to.converter

import com.example.customer.api.to.CustomerTO
import com.example.customer.domain.model.Customer

/** TO ↔ domain mapping for Customer.
  *
  * Lives in `customer/impl/to/converter/` — the only place that sees both
  * sides (the domain entity from this module, the TO from `customer-api`).
  */
object CustomerConverter {

  def toCustomerTO(d: Customer): CustomerTO =
    CustomerTO(d.id, d.email, d.name)

  def toCustomer(to: CustomerTO): Customer =
    Customer(to.id, to.email, to.name)
}
