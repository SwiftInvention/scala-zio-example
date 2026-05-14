package com.example.ctx.notification.impl.to

import com.example.lib.common.domain.model.NewTypes.CustomerId
import zio.schema.{DeriveSchema, Schema}

/** Notification's wire-format view of a customer (id, email, name). */
final case class NotificationRecipientTO(
    id: CustomerId,
    email: String,
    name: String
)

object NotificationRecipientTO {
  implicit val schema: Schema[NotificationRecipientTO] = DeriveSchema.gen[NotificationRecipientTO]
}
