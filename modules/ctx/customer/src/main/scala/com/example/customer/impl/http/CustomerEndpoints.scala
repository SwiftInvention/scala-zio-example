package com.example.customer.impl.http

import com.example.customer.api.to.{AddressTO, CustomerTO}
import com.example.http.api.ApiFailure
import zio.http._
import zio.http.codec.PathCodec
import zio.http.endpoint.Endpoint

/** Typed endpoint definitions for the customer ctx. Pure shape — no implementation. Reused at two sites: by
  * `CustomerRoutes` to provide the implementations, and by `ServerApp` to generate the OpenAPI document. See
  * `patterns/http-endpoints.md` for the discipline (full variant set per endpoint, `ApiFailure.from` mapping).
  */
object CustomerEndpoints {

  import ApiFailure.{
    badRequestCodec,
    forbiddenCodec,
    internalCodec,
    notFoundCodec,
    serviceUnavailableCodec,
    unauthorizedCodec
  }

  val list =
    Endpoint(Method.GET / "customers")
      .out[List[CustomerTO]]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val get =
    Endpoint(Method.GET / "customers" / PathCodec.string("id"))
      .out[CustomerTO]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val listAddressesForCustomer =
    Endpoint(Method.GET / "customers" / PathCodec.string("id") / "addresses")
      .out[List[AddressTO]]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val getAddress =
    Endpoint(Method.GET / "addresses" / PathCodec.string("id"))
      .out[AddressTO]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  /** All endpoints in this ctx. Aggregated by `ServerApp` for OpenAPI generation. */
  val all: List[Endpoint[_, _, _, _, _]] = List(list, get, listAddressesForCustomer, getAddress)
}
