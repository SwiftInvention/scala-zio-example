package com.example.customer.domain.service

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Address

trait AddressService {
  def find(id: AddressId): AppIO[Option[Address]]
  def get(id: AddressId): AppIO[Address]
  def listForCustomer(customerId: CustomerId): AppIO[List[Address]]
}
