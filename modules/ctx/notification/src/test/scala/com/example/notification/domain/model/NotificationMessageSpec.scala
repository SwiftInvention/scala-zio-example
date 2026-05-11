package com.example.notification.domain.model

import com.example.notification.domain.error.InvalidNotificationMessageError
import zio.test.Assertion._
import zio.test._

object NotificationMessageSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("NotificationMessage")(
    suite("apply — valid inputs")(
      test("accepts a typical message") {
        for {
          m <- NotificationMessage("Welcome aboard, Ada.")
        } yield assert(m.value)(equalTo("Welcome aboard, Ada."))
      },
      test("trims surrounding whitespace") {
        for {
          m <- NotificationMessage("   hello   ")
        } yield assert(m.value)(equalTo("hello"))
      },
      test("accepts a single character (min boundary)") {
        for {
          m <- NotificationMessage("x")
        } yield assert(m.value)(equalTo("x"))
      },
      test("accepts max-length input (boundary)") {
        val s = "a" * NotificationMessage.MaxLength
        for {
          m <- NotificationMessage(s)
        } yield assert(m.value.length)(equalTo(NotificationMessage.MaxLength))
      }
    ),
    suite("apply — invalid inputs")(
      test("rejects empty string") {
        assertZIO(NotificationMessage("").exit)(fails(isSubtype[InvalidNotificationMessageError](anything)))
      },
      test("rejects whitespace-only input") {
        assertZIO(NotificationMessage("    ").exit)(fails(isSubtype[InvalidNotificationMessageError](anything)))
      },
      test("rejects input over max length") {
        val s = "a" * (NotificationMessage.MaxLength + 1)
        assertZIO(NotificationMessage(s).exit)(fails(isSubtype[InvalidNotificationMessageError](anything)))
      }
    )
  )
}
