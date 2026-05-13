package com.example.ctx.notification.impl.to.converter

import com.example.ctx.notification.api.to.NotificationTO
import com.example.ctx.notification.domain.model.Notification

/** TO ↔ domain mapping for `Notification`. Channel renders as its enumeratum `entryName`; the parse direction (used on
  * inbound create requests) lives on `NotificationChannel.parse` and is invoked by `NotificationRoutes`.
  */
object NotificationConverter {

  def toNotificationTO(d: Notification): NotificationTO =
    NotificationTO(
      id = d.id,
      recipientId = d.recipientId,
      channel = d.channel.entryName,
      message = d.message.value,
      createdAt = d.createdAt
    )
}
