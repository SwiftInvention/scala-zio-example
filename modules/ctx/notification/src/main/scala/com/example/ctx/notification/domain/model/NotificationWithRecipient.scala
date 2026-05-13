package com.example.ctx.notification.domain.model

/** Domain-level pair returned by `NotificationAppService` for enriched read paths. */
final case class NotificationWithRecipient(
    notification: Notification,
    recipient: NotificationRecipient
)
