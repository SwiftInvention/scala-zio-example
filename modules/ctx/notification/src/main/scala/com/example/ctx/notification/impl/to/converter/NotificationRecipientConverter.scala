package com.example.ctx.notification.impl.to.converter

import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.notification.domain.model.NotificationRecipient
import com.example.ctx.notification.impl.to.NotificationRecipientTO

/** Mappings into and out of `NotificationRecipient`:
  *   - `toNotificationRecipientTO` — domain → outbound wire format
  *   - `fromCustomerTO` — cross-context boundary, projecting `CustomerTO` into notification's recipient view
  */
object NotificationRecipientConverter {

  def toNotificationRecipientTO(d: NotificationRecipient): NotificationRecipientTO =
    NotificationRecipientTO(
      id = d.id,
      email = d.email,
      name = d.name
    )

  def fromCustomerTO(to: CustomerTO): NotificationRecipient =
    NotificationRecipient(
      id = to.id,
      email = to.email,
      name = to.name
    )
}
