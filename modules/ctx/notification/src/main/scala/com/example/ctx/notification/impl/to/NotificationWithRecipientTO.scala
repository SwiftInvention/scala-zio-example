package com.example.ctx.notification.impl.to

import zio.schema.{DeriveSchema, Schema}

/** Composite response pairing a notification with its recipient for list/get endpoints. */
final case class NotificationWithRecipientTO(
    notification: NotificationTO,
    recipient: NotificationRecipientTO
)

object NotificationWithRecipientTO {
  implicit val schema: Schema[NotificationWithRecipientTO] = DeriveSchema.gen[NotificationWithRecipientTO]
}
