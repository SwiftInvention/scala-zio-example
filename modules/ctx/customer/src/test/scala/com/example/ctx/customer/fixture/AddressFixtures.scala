package com.example.ctx.customer.fixture

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.impl.repo.sql.entity.AddressPE
import com.example.lib.db.impl.repo.sql.schema.AddressDbSchema
import zio._

/** Typed test fixtures for `Address`.
  *
  * Address PEs reference customer IDs from `CustomerFixtures` so seed scripts compose: seed the parent customer first,
  * then the child address. `orphanPE` deliberately references a non-existent customer — used by FK-enforcement tests.
  */
object AddressFixtures {

  val adaHomePE: AddressPE = AddressPE(
    id = AddressId("a-ada-home"),
    customerId = CustomerFixtures.adaPE.id,
    line = "85 Royal Street",
    city = "London",
    postalCode = "SE1 7LS"
  )

  val adaWorkPE: AddressPE = AddressPE(
    id = AddressId("a-ada-work"),
    customerId = CustomerFixtures.adaPE.id,
    line = "1 Carlton Gardens",
    city = "London",
    postalCode = "SW1Y 5AA"
  )

  val alanHomePE: AddressPE = AddressPE(
    id = AddressId("a-alan-home"),
    customerId = CustomerFixtures.alanPE.id,
    line = "78 High Street",
    city = "Bletchley",
    postalCode = "MK3 7BS"
  )

  /** An address whose customer_id points at a non-existent customer. Inserting this should fail with an FK violation.
    */
  val orphanPE: AddressPE = AddressPE(
    id = AddressId("a-orphan"),
    customerId = CustomerId("c-doesnt-exist"),
    line = "Nowhere",
    city = "Nullville",
    postalCode = "00000"
  )

  def seed(ctx: SqlContext, pe: AddressPE): AppIO[Unit] = {
    val schema = AddressDbSchema(ctx)
    import ctx._
    val q = quote(schema.addressTable.insertValue(lift(pe)))
    ctx.runQuery(run(q)).unit
  }

  def seedAll(ctx: SqlContext, pes: List[AddressPE]): AppIO[Unit] =
    ZIO.foreachDiscard(pes)(pe => seed(ctx = ctx, pe = pe))
}
