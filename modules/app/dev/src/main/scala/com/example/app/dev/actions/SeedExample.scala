package com.example.app.dev.actions

import java.time.Instant

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.domain.model.Types.AppRIO
import com.example.common.domain.service.Transactor
import com.example.common.impl.config.ConfigBootstrap
import com.example.common.impl.logging.AppLogger
import com.example.common.impl.repo.sql.{DataSourceConfig, DataSourceLayer, SqlContext}
import com.example.common.impl.service.TransactorQuillImpl
import com.example.customer.impl.service.repo.sql.entity.CustomerPE
import com.example.notification.impl.service.repo.sql.entity.NotificationPE
import zio._

/** Seeds the example fixtures used by the smoke test: three customers (Ada, Alan, Grace) and a handful of notifications
  * across them.
  *
  * Writes directly via Quill against the underlying tables. Seeding is infrastructure, not a domain operation, so it
  * skips the service layer and inserts PE rows the same way the repos' writes would.
  *
  * Idempotency: writes run inside a single transaction. If any row already exists, the insert fails on the primary key
  * and the whole batch rolls back. To re-seed cleanly, run `just local-infra-reset && just db-migrate` first.
  *
  * To run: `just seed-example`.
  */
object SeedExample extends ZIOAppDefault {

  override val bootstrap: ZLayer[Any, Any, Any] = AppLogger.bootstrap

  private val customers: List[CustomerPE] = List(
    CustomerPE(id = CustomerId("c-001"), email = "ada@example.test", name = "Ada Lovelace"),
    CustomerPE(id = CustomerId("c-002"), email = "alan@example.test", name = "Alan Turing"),
    CustomerPE(id = CustomerId("c-003"), email = "grace@example.test", name = "Grace Hopper")
  )

  private val notifications: List[NotificationPE] = {
    val t = Instant.parse("2026-05-11T12:00:00Z")
    List(
      NotificationPE(
        id = NotificationId("n-seed-0001"),
        recipientId = CustomerId("c-001"),
        channel = "Email",
        message = "Welcome aboard, Ada.",
        createdAt = t
      ),
      NotificationPE(
        id = NotificationId("n-seed-0002"),
        recipientId = CustomerId("c-002"),
        channel = "Sms",
        message = "Hi Alan — quick note about your account.",
        createdAt = t.plusSeconds(60)
      ),
      NotificationPE(
        id = NotificationId("n-seed-0003"),
        recipientId = CustomerId("c-003"),
        channel = "InApp",
        message = "Grace, you've got mail.",
        createdAt = t.plusSeconds(120)
      )
    )
  }

  val seed: AppRIO[SqlContext & Transactor, Unit] =
    for {
      ctx        <- ZIO.service[SqlContext]
      transactor <- ZIO.service[Transactor]
      _          <- ZIO.logInfo(s"seeding ${customers.size} customers and ${notifications.size} notifications")
      _ <- transactor.withTransaction(insertCustomers(ctx, customers) *> insertNotifications(ctx, notifications))
      _ <- ZIO.logInfo("seed complete")
    } yield ()

  // Quill's `run` collides with `ZIOAppDefault.run`; invoke qualified as `ctx.run(q)`.
  private def insertCustomers(ctx: SqlContext, pes: List[CustomerPE]): AppRIO[Any, Unit] = {
    import ctx.{run => _, _}
    ZIO.foreachDiscard(pes) { pe =>
      val q = quote(querySchema[CustomerPE]("customer").insertValue(lift(pe)))
      ctx.runQuery(ctx.run(q)).unit
    }
  }

  private def insertNotifications(ctx: SqlContext, pes: List[NotificationPE]): AppRIO[Any, Unit] = {
    import ctx.{run => _, _}
    ZIO.foreachDiscard(pes) { pe =>
      val q = quote(querySchema[NotificationPE]("notification").insertValue(lift(pe)))
      ctx.runQuery(ctx.run(q)).unit
    }
  }

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    seed.provide(
      ConfigBootstrap.layer,
      DataSourceConfig.layer,
      DataSourceLayer.layer,
      SqlContext.layer,
      TransactorQuillImpl.layer
    )
}
