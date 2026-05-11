package com.example.customer.impl.http

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.impl.logging.LogError
import com.example.customer.app.CustomerAppService
import com.example.customer.impl.to.converter.AddressConverter.toAddressTO
import com.example.customer.impl.to.converter.CustomerConverter.toCustomerTO
import com.example.http.api.ApiFailure
import zio._
import zio.http._

/** HTTP route implementations for the customer ctx. Wires `CustomerEndpoints` definitions to `CustomerAppService`
  * effects. Splitting endpoint definitions from implementations keeps the wire shape (used by OpenAPI generation) and
  * the behavior (used by the server) at separate sites that can't drift.
  *
  * Error mapping: every endpoint declares the full `ApiFailure` variant set in `CustomerEndpoints`, so each impl uses
  * `mapError(ApiFailure.from)` — matching on `f.asHttpError` picks the right variant by status family.
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
