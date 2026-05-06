package com.example.customer.impl

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.CustomerApi
import com.example.customer.api.to.{AddressTO, CustomerTO}
import com.example.customer.app.CustomerAppService
import com.example.customer.impl.to.converter.AddressConverter.toAddressTO
import com.example.customer.impl.to.converter.CustomerConverter.toCustomerTO
import zio._

/** In-process implementation of `CustomerApi`. Maps domain entities to TOs at the context boundary and delegates the
  * actual work to `CustomerAppService`.
  */
final class CustomerApiDirectImpl(appService: CustomerAppService) extends CustomerApi {
  override def find(id: CustomerId): AppIO[Option[CustomerTO]] =
    appService.find(id).map(_.map(toCustomerTO))

  override def get(id: CustomerId): AppIO[CustomerTO] =
    appService.get(id).map(toCustomerTO)

  override def list: AppIO[List[CustomerTO]] =
    appService.list.map(_.map(toCustomerTO))

  override def findAddress(id: AddressId): AppIO[Option[AddressTO]] =
    appService.findAddress(id).map(_.map(toAddressTO))

  override def getAddress(id: AddressId): AppIO[AddressTO] =
    appService.getAddress(id).map(toAddressTO)

  override def listAddressesForCustomer(customerId: CustomerId): AppIO[List[AddressTO]] =
    appService.listAddressesForCustomer(customerId).map(_.map(toAddressTO))
}

object CustomerApiDirectImpl {
  val layer: URLayer[CustomerAppService, CustomerApi] =
    ZLayer.fromFunction(new CustomerApiDirectImpl(_))
}
