package com.example.notification.impl.service.repo

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.common.domain.service.Transactor
import com.example.common.impl.repo.sql.SqlContext
import com.example.notification.domain.model.Notification
import com.example.notification.domain.service.repo.NotificationRepo
import com.example.notification.impl.service.repo.sql.NotificationDbSchema
import com.example.notification.impl.service.repo.sql.converter.NotificationPEConverter
import zio._

/** MySQL-backed `NotificationRepo`. Each method opens its own transaction via `Transactor.withTransaction` (per the
  * `tx-default` principle).
  */
final class NotificationRepoMySQLImpl(val ctx: SqlContext, transactor: Transactor)
    extends NotificationRepo
    with NotificationDbSchema {
  import ctx._

  import ctx.extras._

  override def insert(notification: Notification): AppIO[Unit] =
    transactor.withTransaction {
      val pe = NotificationPEConverter.toNotificationPE(notification)
      val q  = quote(notificationTable.insertValue(lift(pe)))
      ctx.runQuery(run(q)).unit
    }

  override def find(id: NotificationId): AppIO[Option[Notification]] =
    transactor.withTransaction {
      val q = quote(notificationTable.filter(_.id === lift(id)))
      ctx.runQuerySingleResult(run(q)).flatMap {
        case Some(pe) => NotificationPEConverter.toNotification(pe).map(Some(_))
        case None     => ZIO.none
      }
    }

  override def list: AppIO[List[Notification]] =
    transactor.withTransaction {
      val q = quote(notificationTable)
      ctx.runQuery(run(q)).flatMap(ZIO.foreach(_)(NotificationPEConverter.toNotification))
    }

  override def listForRecipient(recipientId: CustomerId): AppIO[List[Notification]] =
    transactor.withTransaction {
      val q = quote(notificationTable.filter(_.recipientId === lift(recipientId)))
      ctx.runQuery(run(q)).flatMap(ZIO.foreach(_)(NotificationPEConverter.toNotification))
    }
}

object NotificationRepoMySQLImpl {
  val layer: URLayer[SqlContext & Transactor, NotificationRepo] =
    ZLayer.fromFunction(new NotificationRepoMySQLImpl(_, _))
}
