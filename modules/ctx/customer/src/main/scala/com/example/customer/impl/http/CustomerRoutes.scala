package com.example.customer.impl.http

import com.example.common.domain.error.AppFailure
import com.example.common.domain.error.api.{ErrorTO, UnhandledApiError}
import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.customer.api.CustomerApi
import zio._
import zio.http._
import zio.json._

final class CustomerRoutes(api: CustomerApi) {
  val routes: Routes[Any, Response] =
    Routes(
      Method.GET / "customers" -> handler { (_: Request) =>
        api.list
          .map(customers => Response.json(customers.toJson))
          .mapError(toErrorResponse)
      },
      Method.GET / "customers" / string("id") -> handler { (id: String, _: Request) =>
        api
          .get(CustomerId(id))
          .map(customer => Response.json(customer.toJson))
          .mapError(toErrorResponse)
      },
      Method.GET / "customers" / string("id") / "addresses" -> handler { (id: String, _: Request) =>
        api
          .listAddressesForCustomer(CustomerId(id))
          .map(addresses => Response.json(addresses.toJson))
          .mapError(toErrorResponse)
      },
      Method.GET / "addresses" / string("id") -> handler { (id: String, _: Request) =>
        api
          .getAddress(AddressId(id))
          .map(address => Response.json(address.toJson))
          .mapError(toErrorResponse)
      }
    )

  private def toErrorResponse(e: Throwable): Response = e match {
    case f: AppFailure => renderAppFailure(f)
    case other         => renderAppFailure(UnhandledApiError(message = other.getMessage, cause = Some(other)))
  }

  private def renderAppFailure(f: AppFailure): Response =
    Response
      .json(ErrorTO.from(f).toJson)
      .status(Status.fromInt(f.responseCode))
}

object CustomerRoutes {
  val layer: URLayer[CustomerApi, CustomerRoutes] =
    ZLayer.fromFunction(new CustomerRoutes(_))
}
