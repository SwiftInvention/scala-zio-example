package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidEmailError
import zio.test.Assertion._
import zio.test._

object EmailSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("Email")(
    suite("apply — valid inputs")(
      test("accepts a plain well-formed address") {
        for {
          e <- Email("ada@example.test")
        } yield assert(e.value)(equalTo("ada@example.test"))
      },
      test("normalizes whitespace and casing") {
        for {
          e <- Email("  Ada@Example.Test  ")
        } yield assert(e.value)(equalTo("ada@example.test"))
      },
      test("accepts plus-addressing") {
        for {
          e <- Email("ada+work@example.test")
        } yield assert(e.value)(equalTo("ada+work@example.test"))
      },
      test("accepts dotted local part") {
        for {
          e <- Email("ada.lovelace@example.test")
        } yield assert(e.value)(equalTo("ada.lovelace@example.test"))
      },
      test("two equal raw inputs produce equal Emails") {
        for {
          a <- Email("Ada@Example.Test")
          b <- Email("ada@example.test")
        } yield assert(a)(equalTo(b))
      }
    ),
    suite("apply — invalid inputs")(
      test("rejects empty string") {
        assertZIO(Email("").exit)(fails(isSubtype[InvalidEmailError](anything)))
      },
      test("rejects missing @") {
        assertZIO(Email("ada.example.test").exit)(fails(isSubtype[InvalidEmailError](anything)))
      },
      test("rejects missing local part") {
        assertZIO(Email("@example.test").exit)(fails(isSubtype[InvalidEmailError](anything)))
      },
      test("rejects missing domain") {
        assertZIO(Email("ada@").exit)(fails(isSubtype[InvalidEmailError](anything)))
      },
      test("rejects missing TLD") {
        assertZIO(Email("ada@example").exit)(fails(isSubtype[InvalidEmailError](anything)))
      },
      test("rejects whitespace-only input") {
        assertZIO(Email("   ").exit)(fails(isSubtype[InvalidEmailError](anything)))
      }
    )
  )
}
