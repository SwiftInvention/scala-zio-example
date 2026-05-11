package com.example.notification.domain.error

import com.example.common.domain.error.api.{HttpBadRequest, HttpError, HttpInternalServerError, HttpNotFound}
import com.example.common.domain.error.{AppFailure, ErrorCategory}
import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.notification.domain.error.NotificationErrorReason._

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

/** Data-integrity failure on the read path: a notification row references a customer the customer ctx can't produce.
  * The customer ctx has no delete operation, so this represents drift from the schema's implied invariant. Renders as
  * 500 — the request can't be served, but the failure is named, not generic.
  */
final case class OrphanedRecipientError private (message: String, cause: Option[Throwable])
    extends NotificationError(errorReason = OrphanedRecipient, message = message, cause = cause)
    with HttpInternalServerError

object OrphanedRecipientError {
  def withIds(notificationId: NotificationId, recipientId: CustomerId): OrphanedRecipientError =
    OrphanedRecipientError(
      message = s"notification ${NotificationId.unwrap(notificationId)} references customer " +
        s"${CustomerId.unwrap(recipientId)} which does not exist",
      cause = None
    )
}
