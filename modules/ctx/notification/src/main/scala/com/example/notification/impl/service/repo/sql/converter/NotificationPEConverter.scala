package com.example.notification.impl.service.repo.sql.converter

import com.example.common.domain.model.Types.AppIO
import com.example.notification.domain.model.{Notification, NotificationChannel, NotificationMessage}
import com.example.notification.impl.service.repo.sql.entity.NotificationPE

/** PE ↔ domain mapping for `Notification`. See the `pe-converters` principle.
  *
  * `toNotification` is effectful — parsing the channel string and re-validating the message both go through the
  * smart-constructor / parse path. A failure means a row violates a domain invariant (data drift), and propagates as
  * `AppFailure`.
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
