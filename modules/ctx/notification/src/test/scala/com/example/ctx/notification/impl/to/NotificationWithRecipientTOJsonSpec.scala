package com.example.ctx.notification.impl.to

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.test.SnapshotSpec
import zio.test._

/** Locks the composite wire shape — the JSON that clients of `GET /notifications/:id` and `GET /notifications` see.
  * Catches accidental shape changes in either embedded TO at the composition boundary.
  */
object NotificationWithRecipientTOJsonSpec extends ZIOSpecDefault with SnapshotSpec {

  private val one = NotificationWithRecipientTO(
    notification = NotificationTO(
      id = NotificationId("n-11111111-2222-3333-4444-555555555555"),
      recipientId = CustomerId("c-11111111-2222-3333-4444-555555555555"),
      channel = "Email",
      message = "Welcome aboard, Ada.",
      createdAt = Instant.parse("2026-05-11T12:00:00Z")
    ),
    recipient = NotificationRecipientTO(
      id = CustomerId("c-11111111-2222-3333-4444-555555555555"),
      email = "ada@example.test",
      name = "Ada Lovelace"
    )
  )

  override def spec: Spec[Any, Throwable] = suite("NotificationWithRecipientTO JSON wire format")(
    test("single composite matches snapshot") {
      matchesJsonSnapshot(name = "NotificationWithRecipientTO/single.json", value = one)
    }
  )
}
