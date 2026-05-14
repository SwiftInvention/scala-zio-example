package com.example.lib.common.domain.model

import com.example.lib.common.domain.error.domain.InvalidCustomerNameError
import zio.test.Assertion._
import zio.test._

object CustomerNameSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("CustomerName")(
    suite("apply — valid inputs")(
      test("accepts a typical name") {
        for {
          n <- CustomerName("Ada Lovelace")
        } yield assert(n.value)(equalTo("Ada Lovelace"))
      },
      test("trims surrounding whitespace") {
        for {
          n <- CustomerName("   Ada   ")
        } yield assert(n.value)(equalTo("Ada"))
      },
      test("accepts a single character (min boundary)") {
        for {
          n <- CustomerName("A")
        } yield assert(n.value)(equalTo("A"))
      },
      test("accepts max-length input (boundary)") {
        val s = "a" * CustomerName.MaxLength
        for {
          n <- CustomerName(s)
        } yield assert(n.value.length)(equalTo(CustomerName.MaxLength))
      }
    ),
    suite("apply — invalid inputs")(
      test("rejects empty string") {
        assertZIO(CustomerName("").exit)(fails(isSubtype[InvalidCustomerNameError](anything)))
      },
      test("rejects whitespace-only input") {
        assertZIO(CustomerName("    ").exit)(fails(isSubtype[InvalidCustomerNameError](anything)))
      },
      test("rejects input over max length") {
        val s = "a" * (CustomerName.MaxLength + 1)
        assertZIO(CustomerName(s).exit)(fails(isSubtype[InvalidCustomerNameError](anything)))
      }
    )
  )
}
