package com.example.ctx.customer.impl.service.repo.converter

import com.example.ctx.customer.domain.model.{Customer, CustomerName, Email}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.sql.entity.CustomerPE

/** PE ↔ domain mapping for `Customer`. A `toCustomer` failure means a DB row violates a domain invariant — data drift
  * or out-of-band write — and propagates as `AppFailure`.
  */
object CustomerPEConverter {

  def toCustomer(pe: CustomerPE): AppIO[Customer] =
    for {
      email <- Email(pe.email)
      name  <- CustomerName(pe.name)
    } yield Customer(id = pe.id, email = email, name = name)

  def toCustomerPE(d: Customer): CustomerPE =
    CustomerPE(id = d.id, email = d.email.value, name = d.name.value)
}
