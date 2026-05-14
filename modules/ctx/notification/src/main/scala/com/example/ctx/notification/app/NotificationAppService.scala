package com.example.ctx.notification.app

import com.example.ctx.notification.domain.model.{
  Notification,
  NotificationChannel,
  NotificationMessage,
  NotificationWithRecipient
}
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.domain.model.Types.AppIO

/** Public surface of the notification context. Domain-typed. `listForRecipient` returns plain `Notification`s — the
  * recipient is implied by the path on the corresponding HTTP route.
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
