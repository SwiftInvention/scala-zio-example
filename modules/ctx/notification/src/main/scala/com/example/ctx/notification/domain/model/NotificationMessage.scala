package com.example.ctx.notification.domain.model

import com.example.ctx.notification.domain.error.InvalidNotificationMessageError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated notification message body. Canonicalizes whitespace (trim) and enforces `1 <= length <= MaxLength`. See
  * the `smart-constructors` pattern doc.
  */
sealed abstract case class NotificationMessage private (value: String)

object NotificationMessage {
  val MinLength = 1
  val MaxLength = 2000

  def apply(s: String): AppIO[NotificationMessage] = {
    val normalized = s.trim
    val len        = normalized.length
    if (len >= MinLength && len <= MaxLength)
      ZIO.succeed(new NotificationMessage(normalized) {})
    else if (len < MinLength)
      ZIO.fail(InvalidNotificationMessageError(message = "Notification message must not be empty"))
    else
      ZIO.fail(
        InvalidNotificationMessageError(message = s"Notification message length $len exceeds max $MaxLength")
      )
  }
}
