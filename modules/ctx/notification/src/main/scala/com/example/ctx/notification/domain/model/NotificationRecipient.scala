package com.example.ctx.notification.domain.model

import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.{CustomerName, Email}

/** Notification-domain view of a customer (id, email, name). Email and name are typed value objects (shared with
  * `customer` via `lib/common/domain/model/`); the cross-context converter validates at the boundary so anything held
  * here satisfies the same invariants as the source `Customer`.
  */
final case class NotificationRecipient(
    id: CustomerId,
    email: Email,
    name: CustomerName
)
