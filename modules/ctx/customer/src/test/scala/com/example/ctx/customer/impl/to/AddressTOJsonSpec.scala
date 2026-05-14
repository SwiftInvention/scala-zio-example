package com.example.ctx.customer.impl.to

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.test.SnapshotSpec
import zio.test._

object AddressTOJsonSpec extends ZIOSpecDefault with SnapshotSpec {

  // Fixed values — snapshots must be deterministic, so no Gens here.
  private val one = AddressTO(
    id = AddressId("a-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
    customerId = CustomerId("11111111-2222-3333-4444-555555555555"),
    line = "85 Royal Street",
    city = "London",
    postalCode = "SE1 7LS"
  )

  private val many = List(
    one,
    AddressTO(
      id = AddressId("a-ffffffff-1111-2222-3333-444444444444"),
      customerId = CustomerId("11111111-2222-3333-4444-555555555555"),
      line = "1 Carlton Gardens",
      city = "London",
      postalCode = "SW1Y 5AA"
    )
  )

  override def spec: Spec[Any, Throwable] = suite("AddressTO JSON wire format")(
    test("single address matches snapshot") {
      matchesJsonSnapshot(name = "AddressTO/single.json", value = one)
    },
    test("list of addresses matches snapshot") {
      matchesJsonSnapshot(name = "AddressTO/list.json", value = many)
    }
  )
}
