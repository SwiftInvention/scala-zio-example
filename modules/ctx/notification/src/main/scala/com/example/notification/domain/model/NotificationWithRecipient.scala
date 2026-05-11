package com.example.notification.domain.model

/** Domain-level pair returned by `NotificationAppService` for enriched read paths. The route layer maps it to
  * `NotificationWithRecipientTO`. Keeping the pair domain-typed means the app service stays free of cross-context TOs.
  */
final case class NotificationWithRecipient(
    notification: Notification,
    recipient: NotificationRecipient
)
