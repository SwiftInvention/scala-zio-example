package com.example.customer.impl.service.repo.sql.converter

import com.example.customer.domain.model.AddressGen
import zio.test.Assertion._
import zio.test._

object AddressPEConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("AddressPEConverter")(
    test("toAddress ∘ toAddressPE preserves identity (round-trip law)") {
      check(AddressGen.addressGen) { a =>
        for {
          r <- AddressPEConverter.toAddress(AddressPEConverter.toAddressPE(a))
        } yield assert(r)(equalTo(a))
      }
    }
  )
}
