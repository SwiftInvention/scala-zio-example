package com.example.app.server

import com.example.BuildInfo
import com.example.common.http.server.middleware.{RequestLogging, RequestTracing}
import com.example.common.http.server.{HealthEndpoints, HealthRoutes}
import com.example.customer.impl.http.{CustomerEndpoints, CustomerRoutes}
import com.example.notification.impl.http.{NotificationEndpoints, NotificationRoutes}
import zio._
import zio.http._
import zio.http.codec.PathCodec
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}
import zio.telemetry.opentelemetry.tracing.Tracing

/** Composes the server's full route graph. Consumed by `ServerApp` for the running server and by `TestServer` so the
  * integration-test harness exercises the same composition the production server runs.
  *
  * Three planes, each with a different middleware policy:
  *   - Application routes (`CustomerRoutes`) get the full middleware chain: tracing, access log, request id.
  *   - Operational routes (`HealthRoutes`, swagger UI) are served bare — probe and doc-fetch traffic shouldn't flood
  *     traces or access logs.
  *   - The OpenAPI document is built from the aggregated `<Name>Endpoints.all` lists. Doc and served behavior come from
  *     the same `Endpoint` values, so they can't drift.
  */
final class ServerRoutes(
    customerRoutes: CustomerRoutes,
    notificationRoutes: NotificationRoutes,
    healthRoutes: HealthRoutes
) {

  // Title doubles as the OpenAPI JSON filename in the Swagger UI mount (`/docs/<title>.json`); keep it URL-clean
  // (no spaces or special chars).
  private val openApi = OpenAPIGen.fromEndpoints(
    title = "scala-zio-example",
    version = BuildInfo.version,
    endpoints = CustomerEndpoints.all ++ NotificationEndpoints.all ++ HealthEndpoints.all
  )

  private val docsRoutes: Routes[Any, Response] =
    SwaggerUI.routes(PathCodec.empty / "docs", openApi)

  private val instrumentedAppRoutes: Routes[Tracing, Response] =
    (customerRoutes.routes ++ notificationRoutes.routes)
      .@@(RequestTracing.span)
      .@@(RequestLogging.accessLog)
      .@@(RequestLogging.requestId)

  val all: Routes[Tracing, Response] =
    instrumentedAppRoutes ++ healthRoutes.routes ++ docsRoutes
}

object ServerRoutes {
  val layer: URLayer[CustomerRoutes & NotificationRoutes & HealthRoutes, ServerRoutes] =
    ZLayer.fromFunction(new ServerRoutes(_, _, _))
}
