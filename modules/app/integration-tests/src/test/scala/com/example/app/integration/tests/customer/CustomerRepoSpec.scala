package com.example.app.integration.tests.customer

import com.example.ctx.customer.domain.service.repo.CustomerRepo
import com.example.ctx.customer.fixture.CustomerFixtures
import com.example.ctx.customer.impl.service.repo.CustomerRepoMySQLImpl
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.test.IntegrationSpec
import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.test.TestDb
import zio._
import zio.test.Assertion._
import zio.test._

/** Integration tests for `CustomerRepo` against a real MySQL test container.
  *
  * Each test gets a fresh schema via `TestDb.freshSchemaLayer`. Seeding goes through `CustomerFixtures.seed` (Quill),
  * not raw SQL — same write path as production code.
  *
  * Organization: one nested suite per repo method, so failures point at the method under test.
  */
object CustomerRepoSpec extends IntegrationSpec {

  private val testLayer = TestDb.freshSchemaLayer >+> CustomerRepoMySQLImpl.layer

  override def spec: Spec[Any, Throwable] = suite("CustomerRepo (MySQL)")(
    suite("find")(
      test("returns Some when the customer exists") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          result <- ZIO.serviceWithZIO[CustomerRepo](_.find(CustomerFixtures.adaPE.id))
        } yield assert(result.map(_.id))(equalTo(Some(CustomerFixtures.adaPE.id)))).provide(testLayer)
      },
      test("returns None when the customer does not exist") {
        (for {
          result <- ZIO.serviceWithZIO[CustomerRepo](_.find(CustomerId("c-missing")))
        } yield assert(result)(isNone)).provide(testLayer)
      }
    ),
    suite("list")(
      test("returns all seeded customers") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seedAll(ctx = ctx, pes = List(CustomerFixtures.adaPE, CustomerFixtures.alanPE))
          result <- ZIO.serviceWithZIO[CustomerRepo](_.list)
          ids = result.map(_.id).toSet
        } yield assert(ids)(equalTo(Set(CustomerFixtures.adaPE.id, CustomerFixtures.alanPE.id)))).provide(testLayer)
      },
      test("returns empty when the schema is fresh") {
        (for {
          result <- ZIO.serviceWithZIO[CustomerRepo](_.list)
        } yield assert(result)(isEmpty)).provide(testLayer)
      }
    )
  )
}
