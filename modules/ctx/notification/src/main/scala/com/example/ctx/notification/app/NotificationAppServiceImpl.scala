package com.example.ctx.notification.app

import com.example.ctx.customer.api.CustomerApi
import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.notification.domain.error.{NotificationNotFoundError, OrphanedRecipientError}
import com.example.ctx.notification.domain.model.{
  Notification,
  NotificationChannel,
  NotificationMessage,
  NotificationWithRecipient
}
import com.example.ctx.notification.domain.service.repo.NotificationRepo
import com.example.ctx.notification.impl.to.converter.NotificationRecipientConverter
import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Composes notification's repo with the customer cross-context contract.
  *
  *   - **Create** — calls `CustomerApi.get(recipientId)` for the existence check. `CustomerNotFoundError` flows through
  *     unchanged; the HTTP layer renders it as 404.
  *   - **Get / list enrichment** — calls `CustomerApi.getMany` for batched recipient lookup. A miss is data drift,
  *     surfaced as `OrphanedRecipientError` (500). See `patterns/cross-context-call.md`.
  */
final class NotificationAppServiceImpl(
    repo: NotificationRepo,
    customerApi: CustomerApi
) extends NotificationAppService {

  override def create(
      recipientId: CustomerId,
      channel: NotificationChannel,
      message: NotificationMessage
  ): AppIO[NotificationWithRecipient] = {
    val build = for {
      recipientTO <- customerApi.get(recipientId)
      now         <- Clock.instant
      uuid        <- Random.nextUUID
      notification = Notification(
        id = NotificationId(uuid.toString),
        recipientId = recipientId,
        channel = channel,
        message = message,
        createdAt = now
      )
      _ <- repo.insert(notification)
    } yield NotificationWithRecipient(
      notification = notification,
      recipient = NotificationRecipientConverter.fromCustomerTO(recipientTO)
    )

    build.tap(result =>
      ZIO.logAnnotate(
        Set(
          LogAnnotation(key = "notification_id", value = result.notification.id.toString),
          LogAnnotation(key = "recipient_id", value = result.recipient.id.toString),
          LogAnnotation(key = "channel", value = result.notification.channel.entryName)
        )
      )(ZIO.logInfo("notification created"))
    )
  }

  override def get(id: NotificationId): AppIO[NotificationWithRecipient] =
    (for {
      notification <- repo.find(id).someOrFail(NotificationNotFoundError.withId(id))
      recipients   <- customerApi.getMany(Set(notification.recipientId))
      enriched     <- zipOne(notification, recipients)
    } yield enriched).tap(result =>
      ZIO.logAnnotate(
        Set(
          LogAnnotation(key = "notification_id", value = result.notification.id.toString),
          LogAnnotation(key = "recipient_id", value = result.recipient.id.toString)
        )
      )(ZIO.logInfo("notification fetched"))
    )

  override def list: AppIO[List[NotificationWithRecipient]] =
    (for {
      notifications <- repo.list
      recipientIds = notifications.iterator.map(_.recipientId).toSet
      _ <- ZIO.logAnnotate(
        Set(
          LogAnnotation(key = "notification_count", value = notifications.size.toString),
          LogAnnotation(key = "recipient_count", value = recipientIds.size.toString)
        )
      )(ZIO.logDebug("enriching notifications with recipients"))
      recipients <- customerApi.getMany(recipientIds)
      enriched   <- ZIO.foreach(notifications)(zipOne(_, recipients))
    } yield enriched).tap(result =>
      ZIO.logAnnotate(
        Set(
          LogAnnotation(key = "notification_count", value = result.size.toString),
          LogAnnotation(key = "recipient_count", value = result.iterator.map(_.recipient.id).toSet.size.toString)
        )
      )(ZIO.logInfo("listed notifications"))
    )

  override def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]] =
    repo
      .listForRecipient(recipientId)
      .tap(result =>
        ZIO.logAnnotate(
          Set(
            LogAnnotation(key = "recipient_id", value = recipientId.toString),
            LogAnnotation(key = "count", value = result.size.toString)
          )
        )(ZIO.logInfo("listed notifications for recipient"))
      )

  private def zipOne(
      n: Notification,
      recipients: Map[CustomerId, CustomerTO]
  ): AppIO[NotificationWithRecipient] =
    recipients.get(n.recipientId) match {
      case Some(to) =>
        ZIO.succeed(
          NotificationWithRecipient(notification = n, recipient = NotificationRecipientConverter.fromCustomerTO(to))
        )
      case None =>
        ZIO.fail(OrphanedRecipientError.withIds(notificationId = n.id, recipientId = n.recipientId))
    }
}

object NotificationAppServiceImpl {
  val layer: URLayer[NotificationRepo & CustomerApi, NotificationAppService] =
    ZLayer.fromFunction(new NotificationAppServiceImpl(_, _))
}
