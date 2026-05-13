package com.example.ctx.notification.domain.model

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}

final case class Notification(
    id: NotificationId,
    recipientId: CustomerId,
    channel: NotificationChannel,
    message: NotificationMessage,
    createdAt: Instant
)
