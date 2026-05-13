package com.example.ctx.customer.impl.http

import com.example.ctx.customer.app.CustomerAppService
import com.example.ctx.customer.impl.to.converter.AddressConverter.toAddressTO
import com.example.ctx.customer.impl.to.converter.CustomerConverter.toCustomerTO
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.impl.http.ApiFailure
import com.example.lib.common.impl.logging.LogError
import zio._
import zio.http._

/** HTTP routes for the customer ctx. Wires `CustomerEndpoints` to `CustomerAppService`; `AppFailure`s map to
  * `ApiFailure` at the boundary via `mapError(ApiFailure.from)`.
  */
final class CustomerRoutes(appService: CustomerAppService) {

  private val list =
    CustomerEndpoints.list.implement { (_: Unit) =>
      appService.list
        .map(_.map(toCustomerTO))
        .tapError(LogError.tagged("CustomerRoutes.list"))
        .mapError(ApiFailure.from)
    }

  private val get =
    CustomerEndpoints.get.implement { (id: String) =>
      appService
        .get(CustomerId(id))
        .map(toCustomerTO)
        .tapError(LogError.tagged("CustomerRoutes.get"))
        .mapError(ApiFailure.from)
    }

  private val listAddressesForCustomer =
    CustomerEndpoints.listAddressesForCustomer.implement { (id: String) =>
      appService
        .listAddressesForCustomer(CustomerId(id))
        .map(_.map(toAddressTO))
        .tapError(LogError.tagged("CustomerRoutes.listAddressesForCustomer"))
        .mapError(ApiFailure.from)
    }

  private val getAddress =
    CustomerEndpoints.getAddress.implement { (id: String) =>
      appService
        .getAddress(AddressId(id))
        .map(toAddressTO)
        .tapError(LogError.tagged("CustomerRoutes.getAddress"))
        .mapError(ApiFailure.from)
    }

  val routes: Routes[Any, Response] =
    list.toRoutes ++ get.toRoutes ++ listAddressesForCustomer.toRoutes ++ getAddress.toRoutes
}

object CustomerRoutes {
  val layer: URLayer[CustomerAppService, CustomerRoutes] =
    ZLayer.fromFunction(new CustomerRoutes(_))
}
