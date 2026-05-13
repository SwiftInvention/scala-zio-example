package com.example.ctx.notification.domain.model

import scala.collection.immutable

import com.example.ctx.notification.domain.error.InvalidNotificationChannelError
import com.example.lib.common.domain.model.Types.AppIO
import enumeratum.{Enum, EnumEntry}
import zio._

/** Channels a notification can be delivered through. Wire and DB use the enumeratum `entryName` string (`"Email"`,
  * `"Sms"`, `"InApp"`); `parse` is the input-boundary constructor, failing with `InvalidNotificationChannelError` on
  * unknown values.
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
