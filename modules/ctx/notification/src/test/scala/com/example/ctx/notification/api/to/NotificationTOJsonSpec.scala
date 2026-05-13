package com.example.ctx.notification.api.to

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.lib.common.test.SnapshotSpec
import zio.test._

object NotificationTOJsonSpec extends ZIOSpecDefault with SnapshotSpec {

  // Fixed values — snapshots must be deterministic, so no Gens here.
  private val one = NotificationTO(
    id = NotificationId("n-11111111-2222-3333-4444-555555555555"),
    recipientId = CustomerId("c-11111111-2222-3333-4444-555555555555"),
    channel = "Email",
    message = "Welcome aboard, Ada.",
    createdAt = Instant.parse("2026-05-11T12:00:00Z")
  )

  private val many = List(
    one,
    NotificationTO(
      id = NotificationId("n-66666666-7777-8888-9999-aaaaaaaaaaaa"),
      recipientId = CustomerId("c-66666666-7777-8888-9999-aaaaaaaaaaaa"),
      channel = "Sms",
      message = "Reminder: your appointment is tomorrow.",
      createdAt = Instant.parse("2026-05-11T12:01:00Z")
    )
  )

  override def spec: Spec[Any, Throwable] = suite("NotificationTO JSON wire format")(
    test("single notification matches snapshot") {
      matchesJsonSnapshot(name = "NotificationTO/single.json", value = one)
    },
    test("list of notifications matches snapshot") {
      matchesJsonSnapshot(name = "NotificationTO/list.json", value = many)
    }
  )
}
