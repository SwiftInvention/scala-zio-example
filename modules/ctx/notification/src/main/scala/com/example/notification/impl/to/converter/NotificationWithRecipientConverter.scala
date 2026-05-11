package com.example.notification.impl.to.converter

import com.example.notification.api.to.NotificationWithRecipientTO
import com.example.notification.domain.model.NotificationWithRecipient

object NotificationWithRecipientConverter {

  def toNotificationWithRecipientTO(d: NotificationWithRecipient): NotificationWithRecipientTO =
    NotificationWithRecipientTO(
      notification = NotificationConverter.toNotificationTO(d.notification),
      recipient = NotificationRecipientConverter.toNotificationRecipientTO(d.recipient)
    )
}
