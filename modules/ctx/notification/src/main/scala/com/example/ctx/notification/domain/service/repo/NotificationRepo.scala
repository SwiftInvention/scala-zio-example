package com.example.ctx.notification.domain.service.repo

import com.example.ctx.notification.domain.model.Notification
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.domain.model.Types.AppIO

trait NotificationRepo {
  def insert(notification: Notification): AppIO[Unit]
  def find(id: NotificationId): AppIO[Option[Notification]]
  def list: AppIO[List[Notification]]
  def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]]
}
