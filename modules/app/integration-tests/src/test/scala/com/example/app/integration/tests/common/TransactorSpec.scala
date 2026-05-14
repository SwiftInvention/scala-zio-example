package com.example.app.integration.tests.common

import com.example.ctx.customer.domain.service.repo.CustomerRepo
import com.example.ctx.customer.fixture.CustomerFixtures
import com.example.ctx.customer.impl.service.repo.CustomerRepoMySQLImpl
import com.example.lib.common.domain.error.backend.BackendErrorReason
import com.example.lib.common.domain.error.{AppFailure, ErrorCategory, HttpInternalServerError}
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.test.IntegrationSpec
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.test.TestDb
import zio._
import zio.test.Assertion._
import zio.test._

/** Integration tests for `Transactor.withTransaction` against a real MySQL. */
object TransactorSpec extends IntegrationSpec {

  private val testLayer = TestDb.freshSchemaLayer >+> CustomerRepoMySQLImpl.layer

  /** Test-only `AppFailure` for simulating failures inside `withTransaction`. Mixed in with `HttpInternalServerError`
    * to satisfy the self-type.
    */
  private final case class TestFailure(message: String) extends AppFailure(message, None) with HttpInternalServerError {
    val category: ErrorCategory    = ErrorCategory.Backend
    val reason: BackendErrorReason = BackendErrorReason.InternalError
  }

  override def spec: Spec[Any, Throwable] = suite("Transactor.withTransaction (MySQL)")(
    test("rolls back the insert when the transaction effect fails") {
      (for {
        transactor <- ZIO.service[Transactor]
        ctx        <- ZIO.service[SqlContext]
        result <- transactor
          .withTransaction(CustomerFixtures.seed(ctx, CustomerFixtures.adaPE) *> ZIO.fail(TestFailure("simulated")))
          .either
        rows <- ZIO.serviceWithZIO[CustomerRepo](_.list)
      } yield assert(result)(isLeft) && assert(rows)(isEmpty)).provide(testLayer)
    },
    test("commits the insert when the transaction effect succeeds") {
      (for {
        transactor <- ZIO.service[Transactor]
        ctx        <- ZIO.service[SqlContext]
        _          <- transactor.withTransaction(CustomerFixtures.seed(ctx, CustomerFixtures.adaPE))
        rows       <- ZIO.serviceWithZIO[CustomerRepo](_.list)
      } yield assert(rows.map(_.id))(equalTo(List(CustomerFixtures.adaPE.id)))).provide(testLayer)
    },
    test("rolls back ALL inserts when the transaction effect fails after multiple writes") {
      (for {
        transactor <- ZIO.service[Transactor]
        ctx        <- ZIO.service[SqlContext]
        result <- transactor
          .withTransaction(
            CustomerFixtures.seed(ctx, CustomerFixtures.adaPE) *>
              CustomerFixtures.seed(ctx, CustomerFixtures.alanPE) *>
              ZIO.fail(TestFailure("simulated after 2 inserts"))
          )
          .either
        rows <- ZIO.serviceWithZIO[CustomerRepo](_.list)
      } yield assert(result)(isLeft) && assert(rows)(isEmpty)).provide(testLayer)
    },
    suite("nested withTransaction (Quill is reentrant — same fiber reuses outer connection)")(
      test("inner failure NOT caught — outer fails, BOTH inner and outer writes are rolled back") {
        (for {
          transactor <- ZIO.service[Transactor]
          ctx        <- ZIO.service[SqlContext]
          result <- transactor
            .withTransaction(
              CustomerFixtures.seed(ctx, CustomerFixtures.adaPE) *>
                transactor.withTransaction(
                  CustomerFixtures.seed(ctx, CustomerFixtures.alanPE) *> ZIO.fail(TestFailure("inner fails"))
                )
            )
            .either
          rows <- ZIO.serviceWithZIO[CustomerRepo](_.list)
        } yield assert(result)(isLeft) && assert(rows)(isEmpty)).provide(testLayer)
      },
      test(
        "inner failure caught — outer commits, BOTH inner and outer writes are persisted (no savepoint isolation)"
      ) {
        (for {
          transactor <- ZIO.service[Transactor]
          ctx        <- ZIO.service[SqlContext]
          _ <- transactor.withTransaction(
            CustomerFixtures.seed(ctx, CustomerFixtures.adaPE) *>
              transactor
                .withTransaction(
                  CustomerFixtures.seed(ctx, CustomerFixtures.alanPE) *> ZIO.fail(TestFailure("inner fails"))
                )
                .either
                .unit
          )
          rows <- ZIO.serviceWithZIO[CustomerRepo](_.list)
          ids = rows.map(c => CustomerId.unwrap(c.id)).toSet
        } yield assert(ids)(equalTo(Set("c-ada", "c-alan")))).provide(testLayer)
      }
    )
  )
}
