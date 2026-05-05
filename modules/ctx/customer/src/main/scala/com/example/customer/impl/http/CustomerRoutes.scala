package com.example.customer.impl.http

import com.example.common.domain.error.AppFailure
import com.example.common.domain.error.api.{ErrorTO, UnhandledApiError}
import com.example.common.domain.model.NewTypes.CustomerId
import com.example.customer.api.CustomerApi
import com.example.customer.domain.service.repo.AddressRepo
import com.example.customer.impl.to.converter.AddressConverter
import zio._
import zio.http._
import zio.json._

final class CustomerRoutes(api: CustomerApi, addressRepo: AddressRepo) {
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
        addressRepo
          .listForCustomer(CustomerId(id))
          .map(addresses => Response.json(addresses.map(AddressConverter.toAddressTO).toJson))
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
  val layer: URLayer[CustomerApi & AddressRepo, CustomerRoutes] =
    ZLayer.fromFunction(new CustomerRoutes(_, _))
}
