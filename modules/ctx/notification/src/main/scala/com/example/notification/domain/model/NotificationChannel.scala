package com.example.notification.domain.model

import scala.collection.immutable

import com.example.common.domain.model.Types.AppIO
import com.example.notification.domain.error.InvalidNotificationChannelError
import enumeratum.{Enum, EnumEntry}
import zio._

/** Channels a notification can be delivered through. ADT — `Email | Sms | InApp`. Closed set, exhaustive matching.
  *
  * Wire-side and DB-side use the enumeratum `entryName` string (`"Email"`, `"Sms"`, `"InApp"`). Use `parse` at the
  * input boundaries; it fails with `InvalidNotificationChannelError` on unknown values.
  */
sealed trait NotificationChannel extends EnumEntry

object NotificationChannel extends Enum[NotificationChannel] {
  val values: immutable.IndexedSeq[NotificationChannel] = findValues

  case object Email extends NotificationChannel
  case object Sms   extends NotificationChannel
  case object InApp extends NotificationChannel

  def parse(s: String): AppIO[NotificationChannel] =
    withNameOption(s) match {
      case Some(c) => ZIO.succeed(c)
      case None =>
        ZIO.fail(
          InvalidNotificationChannelError(
            message = s"Unknown notification channel: '$s'. Allowed: ${values.map(_.entryName).mkString(", ")}"
          )
        )
    }
}
