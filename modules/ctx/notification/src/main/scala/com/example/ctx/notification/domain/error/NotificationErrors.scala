package com.example.ctx.notification.domain.error

import com.example.ctx.notification.domain.error.NotificationErrorReason._
import com.example.lib.common.domain.error.{AppFailure, ErrorCategory, HttpBadRequest, HttpError, HttpNotFound}
import com.example.lib.common.domain.model.NewTypes.NotificationId

abstract class NotificationError(
    errorReason: NotificationErrorReason,
    message: String,
    cause: Option[Throwable]
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory         = ErrorCategory.Notification
  val reason: NotificationErrorReason = errorReason
}

final case class NotificationNotFoundError private (message: String, cause: Option[Throwable])
    extends NotificationError(errorReason = NotFound, message = message, cause = cause)
    with HttpNotFound

object NotificationNotFoundError {
  def withId(id: NotificationId): NotificationNotFoundError =
    NotificationNotFoundError(
      message = s"Notification with id=${NotificationId.unwrap(id)} is not found",
      cause = None
    )
}

final case class InvalidNotificationMessageError(message: String)
    extends NotificationError(errorReason = InvalidMessage, message = message, cause = None)
    with HttpBadRequest

final case class InvalidNotificationChannelError(message: String)
    extends NotificationError(errorReason = InvalidChannel, message = message, cause = None)
    with HttpBadRequest
