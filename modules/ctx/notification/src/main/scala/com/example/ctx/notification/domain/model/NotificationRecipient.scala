package com.example.ctx.notification.domain.model

import com.example.lib.common.domain.model.NewTypes.CustomerId

/** Notification-domain view of a customer (id, email, name). */
final case class NotificationRecipient(
    id: CustomerId,
    email: String,
    name: String
)
