package com.example.ctx.notification.api.to

import com.example.lib.common.domain.model.NewTypes.CustomerId
import zio.schema.{DeriveSchema, Schema}

/** Create-notification request body. `recipientId` must reference an existing customer — checked at the app-service
  * boundary against `CustomerApi`; `CustomerNotFoundError` propagates as-is.
  */
final case class NotificationCreateRequestTO(
    recipientId: CustomerId,
    channel: String,
    message: String
)

object NotificationCreateRequestTO {
  implicit val schema: Schema[NotificationCreateRequestTO] = DeriveSchema.gen[NotificationCreateRequestTO]
}
