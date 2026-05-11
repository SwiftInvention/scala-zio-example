package com.example.notification.api.to

import com.example.common.domain.model.NewTypes.CustomerId
import zio.schema.{DeriveSchema, Schema}

/** Notification's wire-format view of a customer. Separate from `customer.api.to.CustomerTO` so notification-api and
  * customer-api evolve independently — see `patterns/cross-context-call.md`. The boundary mapping `CustomerTO →
  * NotificationRecipientTO` lives inside the notification ctx.
  */
final case class NotificationRecipientTO(
    id: CustomerId,
    email: String,
    name: String
)

object NotificationRecipientTO {
  implicit val schema: Schema[NotificationRecipientTO] = DeriveSchema.gen[NotificationRecipientTO]
}
