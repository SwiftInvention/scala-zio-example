package com.example.customer.api.to

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.test.SnapshotSpec
import zio.test._

object CustomerTOJsonSpec extends ZIOSpecDefault with SnapshotSpec {

  // Fixed values — snapshots must be deterministic, so no Gens here.
  private val one = CustomerTO(
    id = CustomerId("11111111-2222-3333-4444-555555555555"),
    email = "ada@example.test",
    name = "Ada Lovelace"
  )

  private val many = List(
    one,
    CustomerTO(
      id = CustomerId("66666666-7777-8888-9999-aaaaaaaaaaaa"),
      email = "barbara@example.test",
      name = "Barbara Liskov"
    )
  )

  override def spec: Spec[Any, Throwable] = suite("CustomerTO JSON wire format")(
    test("single customer matches snapshot") {
      matchesJsonSnapshot(name = "CustomerTO/single.json", value = one)
    },
    test("list of customers matches snapshot") {
      matchesJsonSnapshot(name = "CustomerTO/list.json", value = many)
    }
  )
}
