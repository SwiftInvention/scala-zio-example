package com.example.app.integration.tests.common

import com.example.ctx.customer.domain.service.repo.CustomerRepo
import com.example.ctx.customer.fixture.CustomerFixtures
import com.example.ctx.customer.impl.service.repo.CustomerRepoMySQLImpl
import com.example.lib.common.test.IntegrationSpec
import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.test.{TestDb, TestDbIntrospection}
import zio._
import zio.test.Assertion._
import zio.test._

/** Meta-tests for the test infrastructure itself. Verify:
  *   - per-test isolation (one schema's data doesn't leak into another)
  *   - clone faithfulness (the cloned schema's structure matches the template)
  */
object TestDbSpec extends IntegrationSpec {

  private val testLayer = TestDb.freshSchemaLayer >+> CustomerRepoMySQLImpl.layer

  override def spec: Spec[Any, Throwable] = suite("TestDb (meta)")(
    suite("isolation")(
      test("two fresh schemas don't share data — seeding in one is invisible in the other") {
        val seedAndCount =
          (for {
            ctx <- ZIO.service[SqlContext]
            _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
            n   <- ZIO.serviceWithZIO[CustomerRepo](_.list).map(_.size)
          } yield n).provide(testLayer)

        val countOnly =
          (for {
            n <- ZIO.serviceWithZIO[CustomerRepo](_.list).map(_.size)
          } yield n).provide(testLayer)

        for {
          seeded <- seedAndCount
          fresh  <- countOnly
        } yield assert(seeded)(equalTo(1)) && assert(fresh)(equalTo(0))
      }
    ),
    suite("clone faithfulness")(
      test("cloned `customer` table has the same columns as the template") {
        (for {
          ctx          <- ZIO.service[SqlContext]
          clonedCols   <- TestDbIntrospection.listColumns(ctx = ctx, table = "customer")
          templateCols <- TestDbIntrospection.listColumnsInTemplate(table = "customer")
        } yield assert(clonedCols)(equalTo(templateCols))).provide(testLayer)
      },
      test("cloned `address` table has the same columns as the template") {
        (for {
          ctx          <- ZIO.service[SqlContext]
          clonedCols   <- TestDbIntrospection.listColumns(ctx = ctx, table = "address")
          templateCols <- TestDbIntrospection.listColumnsInTemplate(table = "address")
        } yield assert(clonedCols)(equalTo(templateCols))).provide(testLayer)
      },
      test("cloned `address` table preserves the FK to `customer`") {
        (for {
          ctx         <- ZIO.service[SqlContext]
          clonedFks   <- TestDbIntrospection.listForeignKeys(ctx = ctx, table = "address")
          templateFks <- TestDbIntrospection.listForeignKeysInTemplate(table = "address")
        } yield assert(clonedFks)(equalTo(templateFks)) && assert(clonedFks.size)(equalTo(1))).provide(testLayer)
      },
      test("cloned schema is empty (no template data leaked through)") {
        (for {
          rows <- ZIO.serviceWithZIO[CustomerRepo](_.list)
        } yield assert(rows)(isEmpty)).provide(testLayer)
      }
    )
  )
}
