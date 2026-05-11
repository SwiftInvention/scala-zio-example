package com.example.http

import com.example.http.api.ApiFailure
import zio.http._
import zio.http.endpoint.Endpoint

/** Typed endpoint definitions for the operational health probes. K8s probes check status, but the readiness path also
  * carries an `ErrorTO`-shaped body via `ServiceUnavailableResponse` so a human inspecting a failing probe gets the
  * underlying reason without consulting separate logs.
  */
object HealthEndpoints {

  val health =
    Endpoint(Method.GET / "health")
      .out[Unit]

  val ready =
    Endpoint(Method.GET / "ready")
      .out[Unit]
      .outError[ApiFailure.ServiceUnavailableResponse](Status.ServiceUnavailable)

  /** All endpoints in this module. Aggregated by `ServerRoutes` for OpenAPI generation. */
  val all: List[Endpoint[_, _, _, _, _]] = List(health, ready)
}
