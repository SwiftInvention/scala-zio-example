package com.example.customer.api

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.to.{AddressTO, CustomerTO}

trait CustomerApi {
  def find(id: CustomerId): AppIO[Option[CustomerTO]]
  def get(id: CustomerId): AppIO[CustomerTO]
  def list: AppIO[List[CustomerTO]]

  def findAddress(id: AddressId): AppIO[Option[AddressTO]]
  def getAddress(id: AddressId): AppIO[AddressTO]
  def listAddressesForCustomer(customerId: CustomerId): AppIO[List[AddressTO]]
}
