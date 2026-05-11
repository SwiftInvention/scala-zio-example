package com.example.notification.app

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.notification.domain.model.{
  Notification,
  NotificationChannel,
  NotificationMessage,
  NotificationWithRecipient
}

/** Public surface of the notification context. Domain-typed. `listForRecipient` returns plain `Notification`s — the
  * recipient is implied by the path on the corresponding HTTP route. See `NotificationAppServiceImpl` for the
  * cross-context call sites.
  */
trait NotificationAppService {

  def create(
      recipientId: CustomerId,
      channel: NotificationChannel,
      message: NotificationMessage
  ): AppIO[NotificationWithRecipient]

  def get(id: NotificationId): AppIO[NotificationWithRecipient]
  def list: AppIO[List[NotificationWithRecipient]]
  def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]]
}
