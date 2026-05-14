package com.example.ctx.notification.impl.service.repo.converter

import com.example.ctx.notification.domain.model.{Notification, NotificationChannel, NotificationMessage}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.sql.entity.NotificationPE

/** PE ↔ domain mapping for `Notification`. `toNotification` parses the channel string and re-validates the message; a
  * failure means a row violates a domain invariant (data drift).
  */
object NotificationPEConverter {

  def toNotification(pe: NotificationPE): AppIO[Notification] =
    for {
      channel <- NotificationChannel.parse(pe.channel)
      message <- NotificationMessage(pe.message)
    } yield Notification(
      id = pe.id,
      recipientId = pe.recipientId,
      channel = channel,
      message = message,
      createdAt = pe.createdAt
    )

  def toNotificationPE(d: Notification): NotificationPE =
    NotificationPE(
      id = d.id,
      recipientId = d.recipientId,
      channel = d.channel.entryName,
      message = d.message.value,
      createdAt = d.createdAt
    )
}
