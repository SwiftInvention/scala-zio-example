package com.example.customer.app

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.{Address, Customer}
import com.example.customer.domain.service.{AddressService, CustomerService}
import zio._

/** Orchestrator over the customer-ctx domain services. Currently mostly pass-through; cross-entity orchestration (e.g.
  * "delete a customer and its addresses in one transaction") would land here as it accrues.
  */
final class CustomerAppServiceImpl(
    customerService: CustomerService,
    addressService: AddressService
) extends CustomerAppService {
  override def find(id: CustomerId): AppIO[Option[Customer]] = customerService.find(id)
  override def get(id: CustomerId): AppIO[Customer]          = customerService.get(id)
  override def list: AppIO[List[Customer]]                   = customerService.list

  override def findAddress(id: AddressId): AppIO[Option[Address]] = addressService.find(id)
  override def getAddress(id: AddressId): AppIO[Address]          = addressService.get(id)
  override def listAddressesForCustomer(customerId: CustomerId): AppIO[List[Address]] =
    addressService.listForCustomer(customerId)
}

object CustomerAppServiceImpl {
  val layer: URLayer[CustomerService & AddressService, CustomerAppService] =
    ZLayer.fromFunction(new CustomerAppServiceImpl(_, _))
}
