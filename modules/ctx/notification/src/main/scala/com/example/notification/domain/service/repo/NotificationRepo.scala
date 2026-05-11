package com.example.notification.domain.service.repo

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.notification.domain.model.Notification

trait NotificationRepo {
  def insert(notification: Notification): AppIO[Unit]
  def find(id: NotificationId): AppIO[Option[Notification]]
  def list: AppIO[List[Notification]]
  def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]]
}
