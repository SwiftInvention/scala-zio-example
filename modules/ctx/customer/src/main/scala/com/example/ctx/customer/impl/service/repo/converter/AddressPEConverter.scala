package com.example.ctx.customer.impl.service.repo.converter

import com.example.ctx.customer.domain.model.{Address, AddressLine, City, PostalCode}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.sql.entity.AddressPE

/** PE ↔ domain mapping for `Address`. */
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
