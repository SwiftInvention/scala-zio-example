package com.example.ctx.notification.impl.to

import com.example.lib.common.domain.model.NewTypes.CustomerId
import zio.schema.{DeriveSchema, Schema}

/** Create-notification request body. `recipientId` must reference an existing customer; the existence check happens at
  * the app-service boundary, and `CustomerNotFoundError` propagates unchanged on miss.
  */
final case class NotificationCreateRequestTO(
    recipientId: CustomerId,
    channel: String,
    message: String
)

object NotificationCreateRequestTO {
  implicit val schema: Schema[NotificationCreateRequestTO] = DeriveSchema.gen[NotificationCreateRequestTO]
}
