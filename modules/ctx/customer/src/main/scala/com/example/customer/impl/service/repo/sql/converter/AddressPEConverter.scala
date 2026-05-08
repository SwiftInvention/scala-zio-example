package com.example.customer.impl.service.repo.sql.converter

import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.{Address, AddressLine, City, PostalCode}
import com.example.customer.impl.service.repo.sql.entity.AddressPE

/** PE ↔ domain mapping for `Address`.
  *
  * `toAddress` is effectful because the smart constructors validate; a failure means a row violates a domain invariant
  * (data drift or out-of-band write). `toAddressPE` is pure.
  */
object AddressPEConverter {

  def toAddress(pe: AddressPE): AppIO[Address] =
    for {
      line       <- AddressLine(pe.line)
      city       <- City(pe.city)
      postalCode <- PostalCode(pe.postalCode)
    } yield Address(
      id = pe.id,
      customerId = pe.customerId,
      line = line,
      city = city,
      postalCode = postalCode
    )

  def toAddressPE(d: Address): AddressPE =
    AddressPE(
      id = d.id,
      customerId = d.customerId,
      line = d.line.value,
      city = d.city.value,
      postalCode = d.postalCode.value
    )
}
