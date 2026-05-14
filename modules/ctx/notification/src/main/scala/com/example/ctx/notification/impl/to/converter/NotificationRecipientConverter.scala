package com.example.ctx.notification.impl.to.converter

import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.notification.domain.model.NotificationRecipient
import com.example.ctx.notification.impl.to.NotificationRecipientTO
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.common.domain.model.{CustomerName, Email}

/** Mappings into and out of `NotificationRecipient`:
  *   - `toNotificationRecipientTO` — domain → outbound wire format
  *   - `fromCustomerTO` — cross-context boundary, lifting `CustomerTO`'s wire-format strings into notification's typed
  *     recipient projection. Returns `AppIO` because `Email` / `CustomerName` validate at construction; a `CustomerTO`
  *     carrying a value the typed source would have rejected surfaces as the corresponding `InvalidEmailError` /
  *     `InvalidCustomerNameError`.
  */
object NotificationRecipientConverter {

  def toNotificationRecipientTO(d: NotificationRecipient): NotificationRecipientTO =
    NotificationRecipientTO(
      id = d.id,
      email = d.email.value,
      name = d.name.value
    )

  def fromCustomerTO(to: CustomerTO): AppIO[NotificationRecipient] =
    for {
      email <- Email(to.email)
      name  <- CustomerName(to.name)
    } yield NotificationRecipient(id = to.id, email = email, name = name)
}
