package com.example.customer.impl

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.CustomerApi
import com.example.customer.api.to.CustomerTO
import com.example.customer.app.CustomerAppService
import com.example.customer.domain.model.Customer
import zio._

/** In-process implementation of `CustomerApi`. Maps domain entities to TOs at
  * the context boundary and delegates the actual work to `CustomerAppService`.
  */
final class CustomerApiDirectImpl(appService: CustomerAppService) extends CustomerApi {
  override def find(id: CustomerId): AppIO[Option[CustomerTO]] =
    appService.find(id).map(_.map(toTO))

  override def get(id: CustomerId): AppIO[CustomerTO] =
    appService.get(id).map(toTO)

  override def list: AppIO[List[CustomerTO]] =
    appService.list.map(_.map(toTO))

  private def toTO(d: Customer): CustomerTO =
    CustomerTO(d.id, d.email, d.name)
}

object CustomerApiDirectImpl {
  val layer: URLayer[CustomerAppService, CustomerApi] =
    ZLayer.fromFunction(new CustomerApiDirectImpl(_))
}
