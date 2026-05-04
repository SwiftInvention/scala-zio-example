package com.example.customer.impl.service.repo.pg.converter

import com.example.customer.domain.model.Customer
import com.example.customer.impl.service.repo.pg.entity.CustomerPE

/** PE ↔ domain mapping for `Customer`.
  *
  * Mirrors `CustomerConverter` (TO ↔ domain) on the persistence side.
  * Hand-written, one method per direction. See the `pe-converters` principle.
  */
object CustomerPEConverter {

  def toCustomer(pe: CustomerPE): Customer =
    Customer(pe.id, pe.email, pe.name)

  def toCustomerPE(d: Customer): CustomerPE =
    CustomerPE(d.id, d.email, d.name)
}
