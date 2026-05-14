package com.example.ctx.notification.impl.to

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import zio.schema.{DeriveSchema, Schema}

/** Wire format for a notification record.
  *
  * `channel` is a flat string — the domain-side `NotificationChannel` ADT is rendered to its enumeratum `entryName`
  * (`Email | Sms | InApp`) at the boundary. Clients send the same strings on the create path; invalid values fail at
  * parse time with `InvalidNotificationChannelError`.
  */
final case class NotificationTO(
    id: NotificationId,
    recipientId: CustomerId,
    channel: String,
    message: String,
    createdAt: Instant
)

object NotificationTO {
  implicit val schema: Schema[NotificationTO] = DeriveSchema.gen[NotificationTO]
}
