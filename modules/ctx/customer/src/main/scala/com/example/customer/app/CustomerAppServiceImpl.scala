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
  override def find(id: CustomerId): AppIO[Option[Customer]] =
    customerService.find(id).tap {
      case Some(_) => ZIO.logAnnotate(key = "customer_id", value = id.toString)(ZIO.logInfo("customer found"))
      case None    => ZIO.logAnnotate(key = "customer_id", value = id.toString)(ZIO.logInfo("customer not found"))
    }

  override def get(id: CustomerId): AppIO[Customer] =
    customerService
      .get(id)
      .tap(result => ZIO.logAnnotate(key = "customer_id", value = result.id.toString)(ZIO.logInfo("customer fetched")))

  override def list: AppIO[List[Customer]] =
    customerService.list
      .tap(result => ZIO.logAnnotate(key = "count", value = result.size.toString)(ZIO.logInfo("listed customers")))

  override def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]] =
    customerService
      .getMany(ids)
      .tap(result =>
        ZIO.logAnnotate(
          Set(
            LogAnnotation(key = "requested_count", value = ids.size.toString),
            LogAnnotation(key = "found_count", value = result.size.toString)
          )
        )(ZIO.logInfo("customer batch fetch"))
      )

  override def findAddress(id: AddressId): AppIO[Option[Address]] =
    addressService.find(id).tap {
      case Some(_) => ZIO.logAnnotate(key = "address_id", value = id.toString)(ZIO.logInfo("address found"))
      case None    => ZIO.logAnnotate(key = "address_id", value = id.toString)(ZIO.logInfo("address not found"))
    }

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
