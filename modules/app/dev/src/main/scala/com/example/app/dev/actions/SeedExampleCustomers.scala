package com.example.app.dev.actions

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppRIO
import com.example.common.domain.service.Transactor
import com.example.common.impl.config.ConfigBootstrap
import com.example.common.impl.repo.pg.{DataSourceConfig, DataSourceLayer, PgContext}
import com.example.common.impl.service.TransactorQuillImpl
import com.example.customer.impl.service.repo.pg.entity.CustomerPE
import zio._

/** Seeds the example customers used by the smoke test (Ada, Alan, Grace).
  *
  * Writes directly via Quill against the customer table. Seeding is infrastructure, not a domain operation, so it
  * doesn't go through the service layer — it inserts `CustomerPE` rows the same way the repo's writes do.
  *
  * Idempotency: writes are inside a transaction. If rows already exist, the insert fails on the primary key. To re-seed
  * cleanly, run `just db-reset` first.
  *
  * To run: `just seed-example`.
  */
object SeedExampleCustomers extends ZIOAppDefault {

  private val examples: List[CustomerPE] = List(
    CustomerPE(id = CustomerId("c-001"), email = "ada@example.test", name = "Ada Lovelace"),
    CustomerPE(id = CustomerId("c-002"), email = "alan@example.test", name = "Alan Turing"),
    CustomerPE(id = CustomerId("c-003"), email = "grace@example.test", name = "Grace Hopper")
  )

  /** The action's effect, with its env requirement spelled out. Exposed as a val (separate from `run`) so it composes
    * into other entrypoints without dragging the runner's layer stack along.
    */
  val seed: AppRIO[PgContext & Transactor, Unit] =
    for {
      ctx        <- ZIO.service[PgContext]
      transactor <- ZIO.service[Transactor]
      _          <- ZIO.logInfo(s"seeding ${examples.size} example customers")
      _          <- transactor.withTransaction(insertAll(ctx, examples))
      _          <- ZIO.logInfo("seed complete")
    } yield ()

  // Excluding Quill's `run` from the import — it collides with `ZIOAppDefault.run`. We invoke it qualified as
  // `ctx.run(q)` instead.
  private def insertAll(ctx: PgContext, pes: List[CustomerPE]): AppRIO[Any, Unit] = {
    import ctx.{run => _, _}
    ZIO.foreachDiscard(pes) { pe =>
      val q = quote(querySchema[CustomerPE]("customer").insertValue(lift(pe)))
      ctx.runQuery(ctx.run(q)).unit
    }
  }

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    seed.provide(
      ConfigBootstrap.layer,
      DataSourceConfig.layer,
      DataSourceLayer.layer,
      PgContext.layer,
      TransactorQuillImpl.layer
    )
}
