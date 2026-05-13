package com.example.ctx.notification.impl.to.converter

import com.example.ctx.notification.api.to.NotificationWithRecipientTO
import com.example.ctx.notification.domain.model.NotificationWithRecipient

object NotificationWithRecipientConverter {

  def toNotificationWithRecipientTO(d: NotificationWithRecipient): NotificationWithRecipientTO =
    NotificationWithRecipientTO(
      notification = NotificationConverter.toNotificationTO(d.notification),
      recipient = NotificationRecipientConverter.toNotificationRecipientTO(d.recipient)
    )
}
