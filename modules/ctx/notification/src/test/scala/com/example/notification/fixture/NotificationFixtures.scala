package com.example.notification.fixture

import java.time.Instant

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppIO
import com.example.common.impl.repo.sql.SqlContext
import com.example.notification.impl.service.repo.sql.entity.NotificationPE
import zio._

/** Typed test fixtures for `Notification`.
  *
  * Recipient ids are duplicated as string literals here rather than imported from `CustomerFixtures` — bounded contexts
  * don't share test classpaths. The literals are aligned with `customer.fixture.CustomerFixtures` (`c-ada`, `c-alan`)
  * by convention; the FK enforces the match at seed time, so a drift surfaces as a constraint violation, not silently.
  *
  * `orphanPE` references a non-existent customer — used by FK-enforcement and cross-context error-path tests.
  */
object NotificationFixtures {

  private val baseTime: Instant = Instant.parse("2026-05-11T12:00:00Z")

  // Recipient ids — must match the values in `customer.fixture.CustomerFixtures.adaPE.id` / `alanPE.id`.
  val adaRecipientId: CustomerId  = CustomerId("c-ada")
  val alanRecipientId: CustomerId = CustomerId("c-alan")

  val adaEmailPE: NotificationPE = NotificationPE(
    id = NotificationId("n-ada-email"),
    recipientId = adaRecipientId,
    channel = "Email",
    message = "Welcome aboard, Ada.",
    createdAt = baseTime
  )

  val adaSmsPE: NotificationPE = NotificationPE(
    id = NotificationId("n-ada-sms"),
    recipientId = adaRecipientId,
    channel = "Sms",
    message = "Reminder: appointment tomorrow.",
    createdAt = baseTime.plusSeconds(60)
  )

  val alanInAppPE: NotificationPE = NotificationPE(
    id = NotificationId("n-alan-inapp"),
    recipientId = alanRecipientId,
    channel = "InApp",
    message = "Hi Alan — quick note.",
    createdAt = baseTime.plusSeconds(120)
  )

  /** Notification whose recipient_id points at a non-existent customer. Inserting it should fail on the FK. */
  val orphanPE: NotificationPE = NotificationPE(
    id = NotificationId("n-orphan"),
    recipientId = CustomerId("c-doesnt-exist"),
    channel = "Email",
    message = "Nobody home.",
    createdAt = baseTime
  )

  def seed(ctx: SqlContext, pe: NotificationPE): AppIO[Unit] = {
    import ctx._
    val q = quote(querySchema[NotificationPE]("notification").insertValue(lift(pe)))
    ctx.runQuery(run(q)).unit
  }

  def seedAll(ctx: SqlContext, pes: List[NotificationPE]): AppIO[Unit] =
    ZIO.foreachDiscard(pes)(pe => seed(ctx = ctx, pe = pe))
}
