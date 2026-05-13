package com.example.ctx.customer.fixture

import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.impl.sql.entity.CustomerPE
import com.example.lib.db.impl.sql.schema.CustomerDbSchema
import zio._

/** Typed test fixtures for `Customer`.
  *
  * PE values are declared as `val`s — composable, type-checked, and unambiguously the same shape as production data.
  * The `seed` helpers insert via Quill (using the same schema mapping as the repo) so seeding is faithful to actual
  * write paths, not raw SQL with hand-typed column lists.
  *
  * Pattern: each ctx that has integration tests gets a `<Ctx>Fixtures` object with:
  *   - typed PE / domain values for use as inputs and assertion targets
  *   - `seed(pe)` / `seedAll(pes)` helpers
  */
object CustomerFixtures {

  val adaPE: CustomerPE = CustomerPE(
    id = CustomerId("c-ada"),
    email = "ada@example.test",
    name = "Ada Lovelace"
  )

  val alanPE: CustomerPE = CustomerPE(
    id = CustomerId("c-alan"),
    email = "alan@example.test",
    name = "Alan Turing"
  )

  val gracePE: CustomerPE = CustomerPE(
    id = CustomerId("c-grace"),
    email = "grace@example.test",
    name = "Grace Hopper"
  )

  /** Insert a `CustomerPE` into the active schema. Uses Quill so the insert path mirrors the repo's writes.
    *
    * Takes `SqlContext` as a parameter (not a layer) so the resulting `AppIO` has no environment requirement — letting
    * it compose cleanly inside `Transactor.withTransaction(...)`, which expects `AppIO[A]`.
    */
  def seed(ctx: SqlContext, pe: CustomerPE): AppIO[Unit] = {
    val schema = CustomerDbSchema(ctx)
    import ctx._
    val q = quote(schema.customerTable.insertValue(lift(pe)))
    ctx.runQuery(run(q)).unit
  }

  def seedAll(ctx: SqlContext, pes: List[CustomerPE]): AppIO[Unit] =
    ZIO.foreachDiscard(pes)(pe => seed(ctx = ctx, pe = pe))
}
