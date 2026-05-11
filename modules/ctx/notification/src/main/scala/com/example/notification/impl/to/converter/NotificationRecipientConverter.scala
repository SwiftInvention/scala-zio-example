package com.example.notification.impl.to.converter

import com.example.notification.api.to.NotificationRecipientTO
import com.example.notification.domain.model.NotificationRecipient

/** TO mapping for the notification-domain `NotificationRecipient`. Pure rename — `NotificationRecipient` is already the
  * right shape; this converter just translates it to the wire-format type.
  */
object NotificationRecipientConverter {

  def toNotificationRecipientTO(d: NotificationRecipient): NotificationRecipientTO =
    NotificationRecipientTO(
      id = d.id,
      email = d.email,
      name = d.name
    )
}
