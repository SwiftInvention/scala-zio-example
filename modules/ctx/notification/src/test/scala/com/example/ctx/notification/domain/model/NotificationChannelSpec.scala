package com.example.ctx.notification.domain.model

import com.example.ctx.notification.domain.error.InvalidNotificationChannelError
import zio.test.Assertion._
import zio.test._

object NotificationChannelSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("NotificationChannel")(
    suite("parse — valid inputs")(
      test("accepts each declared channel by entryName") {
        check(Gen.elements[NotificationChannel](NotificationChannel.values: _*)) { c =>
          for {
            parsed <- NotificationChannel.parse(c.entryName)
          } yield assert(parsed)(equalTo(c))
        }
      },
      test("Email round-trips through entryName") {
        for {
          parsed <- NotificationChannel.parse("Email")
        } yield assert(parsed)(equalTo(NotificationChannel.Email))
      }
    ),
    suite("parse — invalid inputs")(
      test("rejects empty string") {
        assertZIO(NotificationChannel.parse("").exit)(
          fails(isSubtype[InvalidNotificationChannelError](anything))
        )
      },
      test("rejects unknown channel name") {
        assertZIO(NotificationChannel.parse("Pager").exit)(
          fails(isSubtype[InvalidNotificationChannelError](anything))
        )
      },
      test("is case-sensitive (rejects lowercase)") {
        // enumeratum's default `withNameOption` is case-sensitive; documenting the behavior here so a future loosening
        // surfaces as a test change rather than a silent semantic shift.
        assertZIO(NotificationChannel.parse("email").exit)(
          fails(isSubtype[InvalidNotificationChannelError](anything))
        )
      }
    )
  )
}
