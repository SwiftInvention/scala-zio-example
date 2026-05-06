package com.example.it.http

import com.example.common.domain.error.api.ErrorTO
import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.impl.repo.pg.PgContext
import com.example.customer.api.to.AddressTO
import com.example.customer.fixture.{AddressFixtures, CustomerFixtures}
import zio._
import zio.http._
import zio.test.Assertion._
import zio.test._

/** End-to-end tests for the address HTTP routes. */
object AddressHttpSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("Address HTTP routes")(
    suite("GET /customers/:id/addresses")(
      test("returns all addresses for a customer") {
        (for {
          ctx <- ZIO.service[PgContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _ <- AddressFixtures.seedAll(
            ctx = ctx,
            pes = List(AddressFixtures.adaHomePE, AddressFixtures.adaWorkPE)
          )
          ts <- ZIO.service[TestServer]
          r <- ts.getJson[List[AddressTO]](
            s"/customers/${CustomerId.unwrap(CustomerFixtures.adaPE.id)}/addresses"
          )
          ids = r.body.map(_.id).toSet
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(ids)(equalTo(Set(AddressFixtures.adaHomePE.id, AddressFixtures.adaWorkPE.id))))
          .provide(TestServer.layer)
      },
      test("returns an empty array for a customer with no addresses") {
        (for {
          ctx <- ZIO.service[PgContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.getJson[List[AddressTO]](
            s"/customers/${CustomerId.unwrap(CustomerFixtures.adaPE.id)}/addresses"
          )
        } yield assert(r.status)(equalTo(Status.Ok)) && assert(r.body)(isEmpty)).provide(TestServer.layer)
      }
    ),
    suite("GET /addresses/:id")(
      test("returns the address when present") {
        (for {
          ctx <- ZIO.service[PgContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _   <- AddressFixtures.seed(ctx = ctx, pe = AddressFixtures.adaHomePE)
          ts  <- ZIO.service[TestServer]
          r <- ts.getJson[AddressTO](
            s"/addresses/${AddressId.unwrap(AddressFixtures.adaHomePE.id)}"
          )
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(r.body.id)(equalTo(AddressFixtures.adaHomePE.id))).provide(TestServer.layer)
      },
      test("returns 404 with ErrorTO body when the address is missing") {
        (for {
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[ErrorTO]("/addresses/a-missing")
        } yield assert(r.status)(equalTo(Status.NotFound)) &&
          assert(r.body.code)(equalTo(404)) &&
          assert(r.body.category)(equalTo("Customer")) &&
          assert(r.body.reason)(equalTo("AddressNotFound"))).provide(TestServer.layer)
      }
    )
  )
}
