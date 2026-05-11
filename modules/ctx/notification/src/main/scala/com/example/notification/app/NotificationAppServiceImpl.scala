package com.example.notification.app

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.CustomerApi
import com.example.customer.api.to.CustomerTO
import com.example.notification.domain.error.OrphanedRecipientError
import com.example.notification.domain.model.{
  Notification,
  NotificationChannel,
  NotificationMessage,
  NotificationRecipient,
  NotificationWithRecipient
}
import com.example.notification.domain.service.NotificationService
import zio._

/** Composes notification's domain service with the customer cross-context contract.
  *
  *   - **Create** — calls `CustomerApi.get(recipientId)` for the existence check. `CustomerNotFoundError` flows through
  *     unchanged; the HTTP layer renders it as 404.
  *   - **Get / list enrichment** — calls `CustomerApi.getMany` for batched recipient lookup. A miss is data drift,
  *     surfaced as `OrphanedRecipientError` (500). See `patterns/cross-context-call.md`.
  */
final class NotificationAppServiceImpl(
    notificationService: NotificationService,
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
      created <- notificationService.create(notification)
    } yield NotificationWithRecipient(notification = created, recipient = toRecipient(recipientTO))

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
      notification <- notificationService.get(id)
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
      notifications <- notificationService.list
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
    notificationService
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
        ZIO.succeed(NotificationWithRecipient(notification = n, recipient = toRecipient(to)))
      case None =>
        ZIO.fail(OrphanedRecipientError.withIds(notificationId = n.id, recipientId = n.recipientId))
    }

  private def toRecipient(to: CustomerTO): NotificationRecipient =
    NotificationRecipient(id = to.id, email = to.email, name = to.name)
}

object NotificationAppServiceImpl {
  val layer: URLayer[NotificationService & CustomerApi, NotificationAppService] =
    ZLayer.fromFunction(new NotificationAppServiceImpl(_, _))
}
