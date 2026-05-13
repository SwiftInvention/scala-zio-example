package com.example.ctx.customer.impl

import com.example.ctx.customer.api.CustomerApi
import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.customer.app.CustomerAppService
import com.example.ctx.customer.impl.to.converter.CustomerConverter.toCustomerTO
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** In-process implementation of `CustomerApi`. Maps domain entities to TOs and delegates to `CustomerAppService`. */
final class CustomerApiDirectImpl(appService: CustomerAppService) extends CustomerApi {
  override def get(id: CustomerId): AppIO[CustomerTO] =
    appService.get(id).map(toCustomerTO)

  override def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, CustomerTO]] =
    appService.findMany(ids).map(_.view.mapValues(toCustomerTO).toMap)
}

object CustomerApiDirectImpl {
  val layer: URLayer[CustomerAppService, CustomerApi] =
    ZLayer.fromFunction(new CustomerApiDirectImpl(_))
}
