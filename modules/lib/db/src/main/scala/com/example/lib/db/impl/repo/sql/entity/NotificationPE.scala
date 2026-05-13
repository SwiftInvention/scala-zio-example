package com.example.lib.db.impl.repo.sql.entity

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}

/** Persistence entity for the `notification` table. `channel` is the enumeratum `entryName` of `NotificationChannel`;
  * the converter parses it on read.
  */
final case class NotificationPE(
    id: NotificationId,
    recipientId: CustomerId,
    channel: String,
    message: String,
    createdAt: Instant
)
