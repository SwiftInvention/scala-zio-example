package com.example.notification.domain.model

import java.time.Instant

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}

final case class Notification(
    id: NotificationId,
    recipientId: CustomerId,
    channel: NotificationChannel,
    message: NotificationMessage,
    createdAt: Instant
)
