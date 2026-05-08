package com.example.customer.impl.http

import com.example.common.domain.error.AppFailure
import com.example.common.domain.error.api.ErrorTO
import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.impl.logging.LogError
import com.example.customer.app.CustomerAppService
import com.example.customer.impl.to.converter.AddressConverter.toAddressTO
import com.example.customer.impl.to.converter.CustomerConverter.toCustomerTO
import zio._
import zio.http._
import zio.json._

/** HTTP routes for the customer context. Talks to `CustomerAppService` directly (per `ctx-api`: routes are a separate
  * plane from the cross-context API trait) and converts domain → TO at the response boundary.
  */
final class CustomerRoutes(appService: CustomerAppService) {
  val routes: Routes[Any, Response] =
    Routes(
      Method.GET / "customers" -> handler { (_: Request) =>
        appService.list
          .map(customers => Response.json(customers.map(toCustomerTO).toJson))
          .tapError(LogError.tagged("CustomerRoutes.list"))
          .mapError(renderAppFailure)
      },
      Method.GET / "customers" / string("id") -> handler { (id: String, _: Request) =>
        appService
          .get(CustomerId(id))
          .map(customer => Response.json(toCustomerTO(customer).toJson))
          .tapError(LogError.tagged("CustomerRoutes.get"))
          .mapError(renderAppFailure)
      },
      Method.GET / "customers" / string("id") / "addresses" -> handler { (id: String, _: Request) =>
        appService
          .listAddressesForCustomer(CustomerId(id))
          .map(addresses => Response.json(addresses.map(toAddressTO).toJson))
          .tapError(LogError.tagged("CustomerRoutes.listAddressesForCustomer"))
          .mapError(renderAppFailure)
      },
      Method.GET / "addresses" / string("id") -> handler { (id: String, _: Request) =>
        appService
          .getAddress(AddressId(id))
          .map(address => Response.json(toAddressTO(address).toJson))
          .tapError(LogError.tagged("CustomerRoutes.getAddress"))
          .mapError(renderAppFailure)
      }
    )

  /** Every error in the channel is an `AppFailure` (compiler-enforced via `AppIO`'s error type), so the route boundary
    * just renders — no `Throwable` fallback needed.
    */
  private def renderAppFailure(f: AppFailure): Response =
    Response
      .json(ErrorTO.from(f).toJson)
      .status(Status.fromInt(f.responseCode))
}

object CustomerRoutes {
  val layer: URLayer[CustomerAppService, CustomerRoutes] =
    ZLayer.fromFunction(new CustomerRoutes(_))
}
