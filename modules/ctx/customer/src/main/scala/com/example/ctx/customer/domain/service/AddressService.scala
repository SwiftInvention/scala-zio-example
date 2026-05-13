package com.example.ctx.customer.domain.service

import com.example.ctx.customer.domain.model.Address
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO

trait AddressService {
  def find(id: AddressId): AppIO[Option[Address]]
  def get(id: AddressId): AppIO[Address]
  def listForCustomer(customerId: CustomerId): AppIO[List[Address]]
}
