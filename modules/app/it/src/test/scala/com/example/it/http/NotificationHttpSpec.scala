package com.example.it.http

import com.example.common.domain.error.api.ErrorTO
import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.IntegrationSpec
import com.example.customer.fixture.CustomerFixtures
import com.example.notification.api.to.{NotificationCreateRequestTO, NotificationTO, NotificationWithRecipientTO}
import com.example.notification.fixture.NotificationFixtures
import zio._
import zio.http._
import zio.test.Assertion._
import zio.test._

/** End-to-end tests for the notification HTTP routes.
  *
  * The create path exercises the cross-context call into customer (`CustomerApi.get` for existence check). The list/get
  * paths exercise the cross-context batch fetch (`CustomerApi.getMany`). Error pass-through is verified by the "missing
  * recipient" branch — the 404 body carries `category: Customer`, demonstrating that notification doesn't re-wrap the
  * foreign error. See `patterns/cross-context-call.md`.
  *
  * Coverage gap: `OrphanedRecipientError` (raised by `zipOne` when `getMany` returns an empty map for a referenced id)
  * has no end-to-end test. The FK is `ON DELETE CASCADE`, so the natural reproduction — deleting a customer to leave a
  * dangling notification — can't occur. Reaching it integratively would require swapping in a stub `CustomerApi`.
  * Rendering is locked by `NotificationErrorRenderingSpec`'s snapshot.
  */
object NotificationHttpSpec extends IntegrationSpec {

  override def spec: Spec[Any, Throwable] = suite("Notification HTTP routes")(
    suite("POST /notifications")(
      test("creates a notification and embeds the recipient in the response (201)") {
        // Re-fetches via GET to confirm the row actually landed — a no-op insert that returns the input value would
        // otherwise pass the create-response assertions alone.
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          created <- ts.postJson[NotificationCreateRequestTO, NotificationWithRecipientTO](
            path = "/notifications",
            body = NotificationCreateRequestTO(
              recipientId = CustomerFixtures.adaPE.id,
              channel = "Email",
              message = "Welcome aboard, Ada."
            )
          )
          refetched <- ts.getJson[NotificationWithRecipientTO](
            s"/notifications/${NotificationId.unwrap(created.body.notification.id)}"
          )
        } yield assert(created.status)(equalTo(Status.Created)) &&
          assert(created.body.notification.recipientId)(equalTo(CustomerFixtures.adaPE.id)) &&
          assert(created.body.notification.channel)(equalTo("Email")) &&
          assert(created.body.notification.message)(equalTo("Welcome aboard, Ada.")) &&
          assert(created.body.recipient.id)(equalTo(CustomerFixtures.adaPE.id)) &&
          assert(created.body.recipient.email)(equalTo(CustomerFixtures.adaPE.email)) &&
          assert(refetched.status)(equalTo(Status.Ok)) &&
          assert(refetched.body.notification.id)(equalTo(created.body.notification.id)))
          .provide(TestServer.layer)
      },
      test("returns 404 with category=Customer when recipient doesn't exist (cross-ctx pass-through)") {
        (for {
          ts <- ZIO.service[TestServer]
          r <- ts.postJson[NotificationCreateRequestTO, ErrorTO](
            path = "/notifications",
            body = NotificationCreateRequestTO(
              recipientId = CustomerId("c-doesnt-exist"),
              channel = "Email",
              message = "hello"
            )
          )
        } yield assert(r.status)(equalTo(Status.NotFound)) &&
          assert(r.body.code)(equalTo(404)) &&
          assert(r.body.category)(equalTo("Customer")) &&
          assert(r.body.reason)(equalTo("NotFound"))).provide(TestServer.layer)
      },
      test("returns 400 with reason=InvalidChannel for an unknown channel") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.postJson[NotificationCreateRequestTO, ErrorTO](
            path = "/notifications",
            body = NotificationCreateRequestTO(
              recipientId = CustomerFixtures.adaPE.id,
              channel = "Pager",
              message = "hello"
            )
          )
        } yield assert(r.status)(equalTo(Status.BadRequest)) &&
          assert(r.body.category)(equalTo("Notification")) &&
          assert(r.body.reason)(equalTo("InvalidChannel"))).provide(TestServer.layer)
      },
      test("returns 400 with reason=InvalidMessage for an empty message") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.postJson[NotificationCreateRequestTO, ErrorTO](
            path = "/notifications",
            body = NotificationCreateRequestTO(
              recipientId = CustomerFixtures.adaPE.id,
              channel = "Email",
              message = ""
            )
          )
        } yield assert(r.status)(equalTo(Status.BadRequest)) &&
          assert(r.body.category)(equalTo("Notification")) &&
          assert(r.body.reason)(equalTo("InvalidMessage"))).provide(TestServer.layer)
      }
    ),
    suite("GET /notifications/:id")(
      test("returns the notification with embedded recipient") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _   <- NotificationFixtures.seed(ctx = ctx, pe = NotificationFixtures.adaEmailPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.getJson[NotificationWithRecipientTO](
            s"/notifications/${NotificationId.unwrap(NotificationFixtures.adaEmailPE.id)}"
          )
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(r.body.notification.id)(equalTo(NotificationFixtures.adaEmailPE.id)) &&
          assert(r.body.recipient.id)(equalTo(CustomerFixtures.adaPE.id))).provide(TestServer.layer)
      },
      test("returns 404 with category=Notification when the notification is missing") {
        (for {
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[ErrorTO]("/notifications/n-missing")
        } yield assert(r.status)(equalTo(Status.NotFound)) &&
          assert(r.body.code)(equalTo(404)) &&
          assert(r.body.category)(equalTo("Notification")) &&
          assert(r.body.reason)(equalTo("NotFound"))).provide(TestServer.layer)
      }
    ),
    suite("GET /notifications")(
      test("returns all notifications, each enriched with its recipient (batch fetch)") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _ <- CustomerFixtures.seedAll(
            ctx = ctx,
            pes = List(CustomerFixtures.adaPE, CustomerFixtures.alanPE)
          )
          _ <- NotificationFixtures.seedAll(
            ctx = ctx,
            pes = List(NotificationFixtures.adaEmailPE, NotificationFixtures.alanInAppPE)
          )
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[List[NotificationWithRecipientTO]]("/notifications")
          recipientsByNotificationId = r.body.iterator.map(x => x.notification.id -> x.recipient.id).toMap
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(recipientsByNotificationId)(
            equalTo(
              Map(
                NotificationFixtures.adaEmailPE.id  -> CustomerFixtures.adaPE.id,
                NotificationFixtures.alanInAppPE.id -> CustomerFixtures.alanPE.id
              )
            )
          )).provide(TestServer.layer)
      },
      test("returns an empty array when no notifications exist") {
        (for {
          ts <- ZIO.service[TestServer]
          r  <- ts.getJson[List[NotificationWithRecipientTO]]("/notifications")
        } yield assert(r.status)(equalTo(Status.Ok)) && assert(r.body)(isEmpty)).provide(TestServer.layer)
      }
    ),
    suite("GET /customers/:id/notifications")(
      test("returns notifications scoped to one recipient (no embed)") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _ <- CustomerFixtures.seedAll(
            ctx = ctx,
            pes = List(CustomerFixtures.adaPE, CustomerFixtures.alanPE)
          )
          _ <- NotificationFixtures.seedAll(
            ctx = ctx,
            pes = List(
              NotificationFixtures.adaEmailPE,
              NotificationFixtures.adaSmsPE,
              NotificationFixtures.alanInAppPE
            )
          )
          ts <- ZIO.service[TestServer]
          r <- ts.getJson[List[NotificationTO]](
            s"/customers/${CustomerId.unwrap(CustomerFixtures.adaPE.id)}/notifications"
          )
          ids = r.body.map(_.id).toSet
        } yield assert(r.status)(equalTo(Status.Ok)) &&
          assert(ids)(equalTo(Set(NotificationFixtures.adaEmailPE.id, NotificationFixtures.adaSmsPE.id))))
          .provide(TestServer.layer)
      },
      test("returns an empty array when the recipient has no notifications") {
        (for {
          ctx <- ZIO.service[SqlContext]
          _   <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          ts  <- ZIO.service[TestServer]
          r <- ts.getJson[List[NotificationTO]](
            s"/customers/${CustomerId.unwrap(CustomerFixtures.adaPE.id)}/notifications"
          )
        } yield assert(r.status)(equalTo(Status.Ok)) && assert(r.body)(isEmpty)).provide(TestServer.layer)
      }
    )
  )
}
