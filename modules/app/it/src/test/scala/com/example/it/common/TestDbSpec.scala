package com.example.it.common

import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.{IntegrationSpec, TestDb}
import com.example.customer.domain.service.repo.CustomerRepo
import com.example.customer.fixture.CustomerFixtures
import com.example.customer.impl.service.repo.CustomerRepoMySQLImpl
import zio._
import zio.test.Assertion._
import zio.test._

/** Meta-tests for the test infrastructure itself.
  *
  * These verify that the building blocks tests rely on actually do what they claim:
  *   - per-test isolation (one schema's data doesn't leak into another)
  *   - clone faithfulness (the cloned schema's structure matches the template)
  *
  * If these fail, every other integration test in the codebase becomes suspect.
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
          clonedCols   <- TestDb.listColumns(ctx = ctx, table = "customer")
          templateCols <- TestDb.listColumnsInTemplate(table = "customer")
        } yield assert(clonedCols)(equalTo(templateCols))).provide(testLayer)
      },
      test("cloned `address` table has the same columns as the template") {
        (for {
          ctx          <- ZIO.service[SqlContext]
          clonedCols   <- TestDb.listColumns(ctx = ctx, table = "address")
          templateCols <- TestDb.listColumnsInTemplate(table = "address")
        } yield assert(clonedCols)(equalTo(templateCols))).provide(testLayer)
      },
      test("cloned `address` table preserves the FK to `customer`") {
        (for {
          ctx         <- ZIO.service[SqlContext]
          clonedFks   <- TestDb.listForeignKeys(ctx = ctx, table = "address")
          templateFks <- TestDb.listForeignKeysInTemplate(table = "address")
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
