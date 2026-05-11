package com.example.notification.api.to

import zio.schema.{DeriveSchema, Schema}

/** Composite response for list/get endpoints. Notifications are rarely useful without knowing who they're for, so the
  * route enriches at the boundary. Built by `NotificationAppService` (the composition layer) from a `Notification` and
  * the recipient lookup against `CustomerApi.getMany`.
  */
final case class NotificationWithRecipientTO(
    notification: NotificationTO,
    recipient: NotificationRecipientTO
)

object NotificationWithRecipientTO {
  implicit val schema: Schema[NotificationWithRecipientTO] = DeriveSchema.gen[NotificationWithRecipientTO]
}
