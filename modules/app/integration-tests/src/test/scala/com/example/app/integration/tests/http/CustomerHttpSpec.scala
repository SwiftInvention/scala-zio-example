package com.example.app.integration.tests.http

import com.example.app.integration.tests.TestServer
import com.example.common.domain.error.api.ErrorTO
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.IntegrationSpec
import com.example.customer.api.to.CustomerTO
import com.example.customer.fixture.CustomerFixtures
import zio._
import zio.http._
import zio.test.Assertion._
import zio.test._

/** End-to-end tests for the customer HTTP routes — actual zio-http server bound to an ephemeral port, hit via the real
  * `Client`. Each test gets its own fresh schema via `TestServer.layer`.
  */
object CustomerHttpSpec extends IntegrationSpec {

  override def spec: Spec[Any, Throwable] = suite("Customer HTTP routes")(
    suite("GET /customers")(
      test("returns the seeded customers as a JSON list") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seedAll(ctx = ctx, pes = List(CustomerFixtures.adaPE, CustomerFixtures.alanPE))
          ts  <- ZIO.service[TestServer]
          r   <- ts.getJson[List[CustomerTO]]("/customers")
          ids = r.body.map(_.id).toSet
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(ids)(equalTo(Set(CustomerFixtures.adaPE.id, CustomerFixtures.alanPE.id)))).provide(TestServer.layer)
      },
      test("returns an empty array when no customers exist") {
        (for {
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[List[CustomerTO]]("/customers")
        } yield assert(r.status)(equalTo(Status.Ok)) && assert(r.body)(isEmpty)).provide(TestServer.layer)
      }
    ),
    suite("GET /customers/:id")(
      test("returns the customer when present") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.getJson[CustomerTO](
            s"/customers/${com.example.common.domain.model.NewTypes.CustomerId.unwrap(CustomerFixtures.adaPE.id)}"
          )
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(r.body.id)(equalTo(CustomerFixtures.adaPE.id))).provide(TestServer.layer)
      },
      test("returns 404 with ErrorTO body when the customer is missing") {
        (for {
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[ErrorTO]("/customers/c-missing")
        } yield assert(r.status)(equalTo(Status.NotFound)) &&
          assert(r.body.code)(equalTo(404)) &&
          assert(r.body.category)(equalTo("Customer")) &&
          assert(r.body.reason)(equalTo("NotFound"))).provide(TestServer.layer)
      }
    )
  )
}
