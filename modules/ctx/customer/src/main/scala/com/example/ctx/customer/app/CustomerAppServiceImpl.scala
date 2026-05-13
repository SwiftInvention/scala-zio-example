package com.example.ctx.customer.app

import com.example.ctx.customer.domain.model.{Address, Customer}
import com.example.ctx.customer.domain.service.{AddressService, CustomerService}
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Orchestrator over the customer-ctx domain services. */
final class CustomerAppServiceImpl(
    customerService: CustomerService,
    addressService: AddressService
) extends CustomerAppService {
  override def get(id: CustomerId): AppIO[Customer] =
    customerService
      .get(id)
      .tap(result => ZIO.logAnnotate(key = "customer_id", value = result.id.toString)(ZIO.logInfo("customer fetched")))

  override def list: AppIO[List[Customer]] =
    customerService.list
      .tap(result => ZIO.logAnnotate(key = "count", value = result.size.toString)(ZIO.logInfo("listed customers")))

  override def findMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]] =
    customerService
      .findMany(ids)
      .tap(result =>
        ZIO.logAnnotate(
          Set(
            LogAnnotation(key = "requested_count", value = ids.size.toString),
            LogAnnotation(key = "found_count", value = result.size.toString)
          )
        )(ZIO.logInfo("customer batch fetch"))
      )

  override def getAddress(id: AddressId): AppIO[Address] =
    addressService
      .get(id)
      .tap(result => ZIO.logAnnotate(key = "address_id", value = result.id.toString)(ZIO.logInfo("address fetched")))

  override def listAddressesForCustomer(customerId: CustomerId): AppIO[List[Address]] =
    addressService
      .listForCustomer(customerId)
      .tap(result =>
        ZIO.logAnnotate(
          Set(
            LogAnnotation(key = "customer_id", value = customerId.toString),
            LogAnnotation(key = "count", value = result.size.toString)
          )
        )(ZIO.logInfo("listed addresses for customer"))
      )
}

object CustomerAppServiceImpl {
  val layer: URLayer[CustomerService & AddressService, CustomerAppService] =
    ZLayer.fromFunction(new CustomerAppServiceImpl(_, _))
}
