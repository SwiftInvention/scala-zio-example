package com.example.notification.domain.error

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.impl.http.ErrorTO
import com.example.common.test.SnapshotSpec
import zio.test._

/** Locks the wire shape of `ErrorTO` for each notification-context error. If any error's category, reason, code, or
  * description format changes, the snapshot diff is the alarm. Update the snapshot file deliberately when the change is
  * intended (`SNAPSHOT_UPDATE=true sbt test`).
  */
object NotificationErrorRenderingSpec extends ZIOSpecDefault with SnapshotSpec {

  private val notFound: ErrorTO =
    ErrorTO.from(NotificationNotFoundError.withId(NotificationId("n-11111111-2222-3333-4444-555555555555")))

  private val invalidMessage: ErrorTO =
    ErrorTO.from(InvalidNotificationMessageError(message = "Notification message must not be empty"))

  private val invalidChannel: ErrorTO =
    ErrorTO.from(
      InvalidNotificationChannelError(message = "Unknown notification channel: 'Pager'. Allowed: Email, Sms, InApp")
    )

  private val orphanedRecipient: ErrorTO =
    ErrorTO.from(
      OrphanedRecipientError.withIds(
        notificationId = NotificationId("n-11111111-2222-3333-4444-555555555555"),
        recipientId = CustomerId("c-99999999-9999-9999-9999-999999999999")
      )
    )

  override def spec: Spec[Any, Throwable] = suite("Notification error rendering")(
    test("NotificationNotFoundError → ErrorTO") {
      matchesJsonSnapshot(name = "NotificationErrorRendering/notFound.json", value = notFound)
    },
    test("InvalidNotificationMessageError → ErrorTO") {
      matchesJsonSnapshot(name = "NotificationErrorRendering/invalidMessage.json", value = invalidMessage)
    },
    test("InvalidNotificationChannelError → ErrorTO") {
      matchesJsonSnapshot(name = "NotificationErrorRendering/invalidChannel.json", value = invalidChannel)
    },
    test("OrphanedRecipientError → ErrorTO") {
      matchesJsonSnapshot(name = "NotificationErrorRendering/orphanedRecipient.json", value = orphanedRecipient)
    }
  )
}
