package com.example.customer.impl.service.repo.pg.converter

import com.example.customer.domain.model.CustomerGen
import zio.test.Assertion._
import zio.test._

object CustomerPEConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("CustomerPEConverter")(
    test("toCustomer ∘ toCustomerPE preserves identity (round-trip law)") {
      check(CustomerGen.customerGen) { c =>
        for {
          r <- CustomerPEConverter.toCustomer(CustomerPEConverter.toCustomerPE(c))
        } yield assert(r)(equalTo(c))
      }
    }
  )
}
