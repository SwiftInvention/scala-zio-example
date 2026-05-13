package com.example.ctx.notification.impl.service

import com.example.ctx.notification.domain.error.NotificationNotFoundError
import com.example.ctx.notification.domain.model.Notification
import com.example.ctx.notification.domain.service.NotificationService
import com.example.ctx.notification.domain.service.repo.NotificationRepo
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Pass-through to `NotificationRepo`. Cross-context calls live in `NotificationAppService`. */
final class NotificationServiceImpl(repo: NotificationRepo) extends NotificationService {

  override def create(notification: Notification): AppIO[Notification] =
    repo.insert(notification).as(notification)

  override def find(id: NotificationId): AppIO[Option[Notification]] = repo.find(id)

  override def get(id: NotificationId): AppIO[Notification] =
    repo.find(id).someOrFail(NotificationNotFoundError.withId(id))

  override def list: AppIO[List[Notification]] = repo.list

  override def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]] =
    repo.listForRecipient(recipientId)
}

object NotificationServiceImpl {
  val layer: URLayer[NotificationRepo, NotificationService] =
    ZLayer.fromFunction(new NotificationServiceImpl(_))
}
