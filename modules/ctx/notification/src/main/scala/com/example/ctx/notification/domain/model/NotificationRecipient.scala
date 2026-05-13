package com.example.ctx.notification.domain.model

import com.example.lib.common.domain.model.NewTypes.CustomerId

/** Notification-domain view of a customer (id, email, name). See `patterns/cross-context-call.md` on why notification
  * defines its own type rather than importing customer's.
  */
final case class NotificationRecipient(
    id: CustomerId,
    email: String,
    name: String
)
