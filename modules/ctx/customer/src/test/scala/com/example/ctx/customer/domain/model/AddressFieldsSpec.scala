package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.{InvalidAddressLineError, InvalidCityError, InvalidPostalCodeError}
import zio.test.Assertion._
import zio.test._

/** Unit tests for the three address-line value objects. One nested suite per type. */
object AddressFieldsSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("Address field smart constructors")(
    suite("AddressLine")(
      test("accepts a typical line and trims whitespace") {
        for {
          v <- AddressLine("  221B Baker Street  ")
        } yield assert(v.value)(equalTo("221B Baker Street"))
      },
      test("rejects empty string") {
        assertZIO(AddressLine("").exit)(fails(isSubtype[InvalidAddressLineError](anything)))
      },
      test("rejects whitespace-only input") {
        assertZIO(AddressLine("    ").exit)(fails(isSubtype[InvalidAddressLineError](anything)))
      },
      test("accepts max-length input (boundary)") {
        val s = "a" * AddressLine.MaxLength
        for {
          v <- AddressLine(s)
        } yield assert(v.value.length)(equalTo(AddressLine.MaxLength))
      },
      test("rejects input over max length") {
        val s = "a" * (AddressLine.MaxLength + 1)
        assertZIO(AddressLine(s).exit)(fails(isSubtype[InvalidAddressLineError](anything)))
      }
    ),
    suite("City")(
      test("accepts a typical city and trims whitespace") {
        for {
          v <- City("  London  ")
        } yield assert(v.value)(equalTo("London"))
      },
      test("rejects empty string") {
        assertZIO(City("").exit)(fails(isSubtype[InvalidCityError](anything)))
      },
      test("rejects input over max length") {
        val s = "x" * (City.MaxLength + 1)
        assertZIO(City(s).exit)(fails(isSubtype[InvalidCityError](anything)))
      }
    ),
    suite("PostalCode")(
      test("accepts a typical postal code and trims surrounding whitespace") {
        for {
          v <- PostalCode("  SW1A 2AA  ")
        } yield assert(v.value)(equalTo("SW1A 2AA"))
      },
      test("preserves inner whitespace") {
        for {
          v <- PostalCode("SW1A 2AA")
        } yield assert(v.value)(equalTo("SW1A 2AA"))
      },
      test("rejects empty string") {
        assertZIO(PostalCode("").exit)(fails(isSubtype[InvalidPostalCodeError](anything)))
      },
      test("rejects input over max length") {
        val s = "0" * (PostalCode.MaxLength + 1)
        assertZIO(PostalCode(s).exit)(fails(isSubtype[InvalidPostalCodeError](anything)))
      }
    )
  )
}
