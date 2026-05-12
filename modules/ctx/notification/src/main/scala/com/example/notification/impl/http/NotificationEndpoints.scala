package com.example.notification.impl.http

import com.example.common.http.server.api.ApiFailure
import com.example.notification.api.to.{NotificationCreateRequestTO, NotificationTO, NotificationWithRecipientTO}
import zio.http._
import zio.http.codec.PathCodec
import zio.http.endpoint.Endpoint

/** Typed endpoint definitions for the notification ctx. Pure shape — no implementation. Same discipline as
  * `CustomerEndpoints`: every endpoint declares the full `ApiFailure` variant set so the framework always has a codec
  * for whatever the impl ends up producing. See `patterns/http-endpoints.md`.
  */
object NotificationEndpoints {

  import ApiFailure.{
    badRequestCodec,
    forbiddenCodec,
    internalCodec,
    notFoundCodec,
    serviceUnavailableCodec,
    unauthorizedCodec
  }

  val create =
    Endpoint(Method.POST / "notifications")
      .in[NotificationCreateRequestTO]
      .out[NotificationWithRecipientTO](Status.Created)
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val get =
    Endpoint(Method.GET / "notifications" / PathCodec.string("id"))
      .out[NotificationWithRecipientTO]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val list =
    Endpoint(Method.GET / "notifications")
      .out[List[NotificationWithRecipientTO]]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  val listForRecipient =
    Endpoint(Method.GET / "customers" / PathCodec.string("id") / "notifications")
      .out[List[NotificationTO]]
      .outErrors[ApiFailure](
        badRequestCodec,
        unauthorizedCodec,
        forbiddenCodec,
        notFoundCodec,
        internalCodec,
        serviceUnavailableCodec
      )

  /** All endpoints in this ctx. Aggregated by `ServerRoutes` for OpenAPI generation. */
  val all: List[Endpoint[_, _, _, _, _]] = List(create, get, list, listForRecipient)
}
