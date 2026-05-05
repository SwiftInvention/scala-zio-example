package com.example.customer.domain.service.repo

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Address

trait AddressRepo {
  def find(id: AddressId): AppIO[Option[Address]]
  def listForCustomer(customerId: CustomerId): AppIO[List[Address]]
}
