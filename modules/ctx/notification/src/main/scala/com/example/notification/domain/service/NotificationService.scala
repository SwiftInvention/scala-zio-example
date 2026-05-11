package com.example.notification.domain.service

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.notification.domain.model.Notification

trait NotificationService {
  def create(notification: Notification): AppIO[Notification]
  def find(id: NotificationId): AppIO[Option[Notification]]
  def get(id: NotificationId): AppIO[Notification]
  def list: AppIO[List[Notification]]
  def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]]
}
