package com.example.ctx.notification.domain.service

import com.example.ctx.notification.domain.model.Notification
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.domain.model.Types.AppIO

trait NotificationService {
  def create(notification: Notification): AppIO[Notification]
  def find(id: NotificationId): AppIO[Option[Notification]]
  def get(id: NotificationId): AppIO[Notification]
  def list: AppIO[List[Notification]]
  def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]]
}
