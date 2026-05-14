package com.example.ctx.customer.domain.service.repo

import com.example.ctx.customer.domain.model.Address
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO

trait AddressRepo {
  def find(id: AddressId): AppIO[Option[Address]]
  def listForCustomer(customerId: CustomerId): AppIO[List[Address]]
}
