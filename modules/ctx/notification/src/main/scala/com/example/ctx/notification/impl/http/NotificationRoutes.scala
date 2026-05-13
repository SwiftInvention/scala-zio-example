package com.example.ctx.notification.impl.http

import com.example.ctx.notification.api.to.NotificationCreateRequestTO
import com.example.ctx.notification.app.NotificationAppService
import com.example.ctx.notification.domain.model.{NotificationChannel, NotificationMessage}
import com.example.ctx.notification.impl.to.converter.NotificationConverter.toNotificationTO
import com.example.ctx.notification.impl.to.converter.NotificationWithRecipientConverter.toNotificationWithRecipientTO
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.impl.http.ApiFailure
import com.example.lib.common.impl.logging.LogError
import zio._
import zio.http._

/** HTTP route implementations for the notification ctx. Wires `NotificationEndpoints` to `NotificationAppService`.
  *
  * Cross-context calls don't appear in this file — they live in `NotificationAppService`. The route parses inbound TOs
  * to domain types (channel + message; recipient id passes through), calls the app service, and renders the domain
  * return values back to TOs.
  *
  * Failures (from local validation, the customer existence check, or anything downstream) all enter the same
  * `AppFailure` channel and are mapped via `ApiFailure.from`. `CustomerNotFoundError` reaches here from the customer
  * ctx unchanged — see `patterns/cross-context-call.md`.
  */
final class NotificationRoutes(appService: NotificationAppService) {

  private val create =
    NotificationEndpoints.create.implement { (req: NotificationCreateRequestTO) =>
      (for {
        channel <- NotificationChannel.parse(req.channel)
        message <- NotificationMessage(req.message)
        result  <- appService.create(recipientId = req.recipientId, channel = channel, message = message)
      } yield toNotificationWithRecipientTO(result))
        .tapError(LogError.tagged("NotificationRoutes.create"))
        .mapError(ApiFailure.from)
    }

  private val get =
    NotificationEndpoints.get.implement { (id: String) =>
      appService
        .get(NotificationId(id))
        .map(toNotificationWithRecipientTO)
        .tapError(LogError.tagged("NotificationRoutes.get"))
        .mapError(ApiFailure.from)
    }

  private val list =
    NotificationEndpoints.list.implement { (_: Unit) =>
      appService.list
        .map(_.map(toNotificationWithRecipientTO))
        .tapError(LogError.tagged("NotificationRoutes.list"))
        .mapError(ApiFailure.from)
    }

  private val listForRecipient =
    NotificationEndpoints.listForRecipient.implement { (id: String) =>
      appService
        .listForRecipient(CustomerId(id))
        .map(_.map(toNotificationTO))
        .tapError(LogError.tagged("NotificationRoutes.listForRecipient"))
        .mapError(ApiFailure.from)
    }

  val routes: Routes[Any, Response] =
    create.toRoutes ++ get.toRoutes ++ list.toRoutes ++ listForRecipient.toRoutes
}

object NotificationRoutes {
  val layer: URLayer[NotificationAppService, NotificationRoutes] =
    ZLayer.fromFunction(new NotificationRoutes(_))
}
