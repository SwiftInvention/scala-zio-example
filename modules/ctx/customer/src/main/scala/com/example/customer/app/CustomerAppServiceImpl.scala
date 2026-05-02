package com.example.customer.app

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Customer
import com.example.customer.domain.service.CustomerService
import zio._

/** Pass-through to `CustomerService`. Future ops that orchestrate multiple
  * domain services land here.
  */
final class CustomerAppServiceImpl(service: CustomerService) extends CustomerAppService {
  override def find(id: CustomerId): AppIO[Option[Customer]] = service.find(id)
  override def get(id: CustomerId): AppIO[Customer]          = service.get(id)
  override def list: AppIO[List[Customer]]                   = service.list
}

object CustomerAppServiceImpl {
  val layer: URLayer[CustomerService, CustomerAppService] =
    ZLayer.fromFunction(new CustomerAppServiceImpl(_))
}
