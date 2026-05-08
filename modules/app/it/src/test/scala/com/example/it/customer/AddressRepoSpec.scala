package com.example.it.customer

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.{IntegrationSpec, TestDb}
import com.example.customer.domain.service.repo.AddressRepo
import com.example.customer.fixture.{AddressFixtures, CustomerFixtures}
import com.example.customer.impl.service.repo.AddressRepoMySQLImpl
import zio._
import zio.test.Assertion._
import zio.test._

/** Integration tests for `AddressRepo` against MySQL.
  *
  * Each test seeds parent customers first (FK requirement), then addresses. Demonstrates the FK relationship is
  * enforced and queryable.
  */
object AddressRepoSpec extends IntegrationSpec {

  private val testLayer = TestDb.freshSchemaLayer >+> AddressRepoMySQLImpl.layer

  override def spec: Spec[Any, Throwable] = suite("AddressRepo (MySQL)")(
    suite("find")(
      test("returns Some when the address exists") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _      <- AddressFixtures.seed(ctx = ctx, pe = AddressFixtures.adaHomePE)
          result <- ZIO.serviceWithZIO[AddressRepo](_.find(AddressFixtures.adaHomePE.id))
        } yield assert(result.map(_.id))(equalTo(Some(AddressFixtures.adaHomePE.id)))).provide(testLayer)
      },
      test("returns None when the address does not exist") {
        (for {
          result <- ZIO.serviceWithZIO[AddressRepo](_.find(AddressId("a-missing")))
        } yield assert(result)(isNone)).provide(testLayer)
      }
    ),
    suite("listForCustomer")(
      test("returns all addresses for a customer (multiple)") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seedAll(ctx = ctx, pes = List(CustomerFixtures.adaPE, CustomerFixtures.alanPE))
          _ <- AddressFixtures.seedAll(
            ctx = ctx,
            pes = List(AddressFixtures.adaHomePE, AddressFixtures.adaWorkPE, AddressFixtures.alanHomePE)
          )
          result <- ZIO.serviceWithZIO[AddressRepo](_.listForCustomer(CustomerFixtures.adaPE.id))
          ids = result.map(_.id).toSet
        } yield assert(ids)(equalTo(Set(AddressFixtures.adaHomePE.id, AddressFixtures.adaWorkPE.id))))
          .provide(testLayer)
      },
      test("returns empty when the customer has no addresses") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          result <- ZIO.serviceWithZIO[AddressRepo](_.listForCustomer(CustomerFixtures.adaPE.id))
        } yield assert(result)(isEmpty)).provide(testLayer)
      },
      test("returns empty when the customer doesn't exist (no error — just no matches)") {
        (for {
          result <- ZIO.serviceWithZIO[AddressRepo](_.listForCustomer(CustomerId("c-nobody")))
        } yield assert(result)(isEmpty)).provide(testLayer)
      }
    ),
    suite("foreign key enforcement")(
      test("inserting an address with a non-existent customer fails (FK violation)") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          result <- AddressFixtures.seed(ctx = ctx, pe = AddressFixtures.orphanPE).either
        } yield assert(result)(isLeft)).provide(testLayer)
      },
      test("ON DELETE CASCADE: deleting a customer drops their addresses") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _      <- AddressFixtures.seed(ctx = ctx, pe = AddressFixtures.adaHomePE)
          _      <- TestDb.runSql(s"DELETE FROM customer WHERE id = '${CustomerId.unwrap(CustomerFixtures.adaPE.id)}'")
          result <- ZIO.serviceWithZIO[AddressRepo](_.listForCustomer(CustomerFixtures.adaPE.id))
        } yield assert(result)(isEmpty)).provide(testLayer)
      }
    )
  )
}
