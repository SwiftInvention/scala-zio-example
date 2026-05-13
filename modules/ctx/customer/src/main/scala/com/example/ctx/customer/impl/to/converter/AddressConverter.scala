package com.example.ctx.customer.impl.to.converter

import com.example.ctx.customer.api.to.AddressTO
import com.example.ctx.customer.domain.model.{Address, AddressLine, City, PostalCode}
import com.example.lib.common.domain.model.Types.AppIO

/** TO ↔ domain mapping for `Address`. `toAddress` is effectful because the smart constructors validate; `toAddressTO`
  * is pure (`.value` extraction never fails).
  */
object AddressConverter {

  def toAddressTO(d: Address): AddressTO =
    AddressTO(
      id = d.id,
      customerId = d.customerId,
      line = d.line.value,
      city = d.city.value,
      postalCode = d.postalCode.value
    )

  def toAddress(to: AddressTO): AppIO[Address] =
    for {
      line       <- AddressLine(to.line)
      city       <- City(to.city)
      postalCode <- PostalCode(to.postalCode)
    } yield Address(
      id = to.id,
      customerId = to.customerId,
      line = line,
      city = city,
      postalCode = postalCode
    )
}
