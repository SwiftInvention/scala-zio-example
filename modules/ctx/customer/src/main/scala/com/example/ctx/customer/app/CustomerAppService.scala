package com.example.ctx.customer.app

import com.example.ctx.customer.domain.model.{Address, Customer}
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO

/** Public API surface of the customer context, declared in domain terms.
  *
  * The cross-context contract in `customer-api` (TO-typed) is implemented by a thin adapter (`CustomerApiDirectImpl`)
  * that maps domain → TO at the boundary and delegates to this service.
  */
trait CustomerAppService {
  def get(id: CustomerId): AppIO[Customer]
  def list: AppIO[List[Customer]]
  def findMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]]

  def getAddress(id: AddressId): AppIO[Address]
  def listAddressesForCustomer(customerId: CustomerId): AppIO[List[Address]]
}
