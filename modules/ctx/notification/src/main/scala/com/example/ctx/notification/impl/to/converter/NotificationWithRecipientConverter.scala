package com.example.ctx.notification.impl.to.converter

import com.example.ctx.notification.domain.model.NotificationWithRecipient
import com.example.ctx.notification.impl.to.NotificationWithRecipientTO

object NotificationWithRecipientConverter {

  def toNotificationWithRecipientTO(d: NotificationWithRecipient): NotificationWithRecipientTO =
    NotificationWithRecipientTO(
      notification = NotificationConverter.toNotificationTO(d.notification),
      recipient = NotificationRecipientConverter.toNotificationRecipientTO(d.recipient)
    )
}
