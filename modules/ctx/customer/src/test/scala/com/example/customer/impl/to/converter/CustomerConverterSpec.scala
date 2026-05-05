package com.example.customer.impl.to.converter

import com.example.customer.domain.model.CustomerGen
import zio.test.Assertion._
import zio.test._

object CustomerConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("CustomerConverter")(
    test("toCustomer ∘ toCustomerTO preserves identity (round-trip law)") {
      check(CustomerGen.customerGen) { c =>
        for {
          r <- CustomerConverter.toCustomer(CustomerConverter.toCustomerTO(c))
        } yield assert(r)(equalTo(c))
      }
    }
  )
}
