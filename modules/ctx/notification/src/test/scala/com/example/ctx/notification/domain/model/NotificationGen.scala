package com.example.ctx.notification.domain.model

import java.time.Instant

import com.example.lib.common.domain.model.NewTypes.{CustomerId, NotificationId}
import zio.test._

/** zio-test generators for `Notification` and its parts.
  *
  * Inputs are bounded so smart-constructor invariants always hold — `messageGen` only produces strings in `[MinLength,
  * MaxLength]`, `channelGen` picks from the closed ADT. Failing a smart constructor inside a Gen would surface as a
  * hung shrink, so we generate by parts and lift through `Gen.fromZIO`.
  */
object NotificationGen {

  val notificationIdGen: Gen[Any, NotificationId] =
    Gen.alphaNumericStringBounded(min = 1, max = 30).map(NotificationId(_))

  val recipientIdGen: Gen[Any, CustomerId] =
    Gen.alphaNumericStringBounded(min = 1, max = 30).map(CustomerId(_))

  val channelGen: Gen[Any, NotificationChannel] =
    Gen.elements[NotificationChannel](NotificationChannel.values: _*)

  val messageGen: Gen[Any, NotificationMessage] =
    Gen
      .alphaNumericStringBounded(min = NotificationMessage.MinLength, max = NotificationMessage.MaxLength)
      .flatMap(s => Gen.fromZIO(NotificationMessage(s).orDie))

  /** Generates `Instant`s with millisecond precision — matches MySQL `DATETIME(3)`, so round-trip-through-PE tests
    * don't fail on sub-millisecond rounding. Range covers epoch through ≈2096 (4×10¹² ms).
    */
  val createdAtGen: Gen[Any, Instant] =
    Gen.long(min = 0L, max = 4_000_000_000_000L).map(Instant.ofEpochMilli)

  val notificationGen: Gen[Any, Notification] =
    for {
      id          <- notificationIdGen
      recipientId <- recipientIdGen
      channel     <- channelGen
      message     <- messageGen
      createdAt   <- createdAtGen
    } yield Notification(
      id = id,
      recipientId = recipientId,
      channel = channel,
      message = message,
      createdAt = createdAt
    )

  val recipientGen: Gen[Any, NotificationRecipient] =
    for {
      id    <- recipientIdGen
      email <- Gen.alphaNumericStringBounded(min = 1, max = 40).map(s => s"$s@example.test")
      name  <- Gen.alphaNumericStringBounded(min = 1, max = 60)
    } yield NotificationRecipient(id = id, email = email, name = name)

  val notificationWithRecipientGen: Gen[Any, NotificationWithRecipient] =
    for {
      n <- notificationGen
      r <- recipientGen
    } yield NotificationWithRecipient(notification = n.copy(recipientId = r.id), recipient = r)
}
