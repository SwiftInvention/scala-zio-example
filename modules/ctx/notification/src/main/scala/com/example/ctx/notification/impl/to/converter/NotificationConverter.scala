package com.example.ctx.notification.impl.to.converter

import com.example.ctx.notification.domain.model.Notification
import com.example.ctx.notification.impl.to.NotificationTO

/** Domain → TO mapping for `Notification`. Channel renders as its enumeratum `entryName`. */
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
