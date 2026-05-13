package com.example.ctx.customer.impl.service.repo.converter

import com.example.ctx.customer.domain.model.{Customer, CustomerName, Email}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.repo.sql.entity.CustomerPE

/** PE ↔ domain mapping for `Customer`. Hand-written, one method per direction. See the `pe-converters` principle.
  *
  * `toCustomer` is effectful because the smart constructors validate. A failure here means a row in the DB violates a
  * domain invariant — i.e. data drift or out-of-band write. The smart constructor's `AppFailure` propagates through the
  * `AppIO` channel; the route boundary renders it as a 500-class problem (validation errors carry `HttpBadRequest`, but
  * for read paths "DB row failed validation" is server-side data integrity, not user input).
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
