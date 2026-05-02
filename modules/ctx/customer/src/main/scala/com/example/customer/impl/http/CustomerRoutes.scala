package com.example.customer.impl.http

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
          .mapError(e => Response.internalServerError(e.getMessage))
      }
    )
}

object CustomerRoutes {
  val layer: URLayer[CustomerApi, CustomerRoutes] =
    ZLayer.fromFunction(new CustomerRoutes(_))
}
