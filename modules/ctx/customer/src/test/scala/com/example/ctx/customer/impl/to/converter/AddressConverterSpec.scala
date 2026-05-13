package com.example.ctx.customer.impl.to.converter

import com.example.ctx.customer.domain.model.AddressGen
import zio.test.Assertion._
import zio.test._

object AddressConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("AddressConverter")(
    test("toAddress ∘ toAddressTO preserves identity (round-trip law)") {
      check(AddressGen.addressGen) { a =>
        for {
          r <- AddressConverter.toAddress(AddressConverter.toAddressTO(a))
        } yield assert(r)(equalTo(a))
      }
    }
  )
}
