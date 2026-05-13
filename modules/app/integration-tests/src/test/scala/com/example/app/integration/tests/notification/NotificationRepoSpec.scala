package com.example.app.integration.tests.notification

import com.example.common.domain.model.NewTypes.{CustomerId, NotificationId}
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.test.{IntegrationSpec, TestDb}
import com.example.customer.fixture.CustomerFixtures
import com.example.notification.domain.service.repo.NotificationRepo
import com.example.notification.fixture.NotificationFixtures
import com.example.notification.impl.service.repo.NotificationRepoMySQLImpl
import com.example.notification.impl.service.repo.sql.converter.NotificationPEConverter
import zio._
import zio.test.Assertion._
import zio.test._

/** Integration tests for `NotificationRepo` against a real MySQL test container.
  *
  * Each test gets a fresh schema via `TestDb.freshSchemaLayer`. Notification rows require a parent customer to exist
  * (FK), so suites seed `CustomerFixtures.adaPE` / `alanPE` before inserting notifications.
  *
  * Organization: one nested suite per repo method.
  */
object NotificationRepoSpec extends IntegrationSpec {

  private val testLayer = TestDb.freshSchemaLayer >+> NotificationRepoMySQLImpl.layer

  override def spec: Spec[Any, Throwable] = suite("NotificationRepo (MySQL)")(
    suite("insert")(
      test("persists a notification that can be read back") {
        (for {
          ctx          <- ZIO.service[SqlContext]
          _            <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          notification <- NotificationPEConverter.toNotification(NotificationFixtures.adaEmailPE)
          _            <- ZIO.serviceWithZIO[NotificationRepo](_.insert(notification))
          result       <- ZIO.serviceWithZIO[NotificationRepo](_.find(NotificationFixtures.adaEmailPE.id))
        } yield assert(result.map(_.id))(equalTo(Some(NotificationFixtures.adaEmailPE.id)))).provide(testLayer)
      },
      test("fails when recipient_id references a non-existent customer (FK violation)") {
        // The FK on `notification.recipient_id` references `customer.id`; inserting against a missing parent
        // raises a SQL constraint violation that the `Transactor` boundary wraps into the `AppFailure` channel.
        // The precise wrapping is `TransactorSpec`'s concern; here we just confirm the FK is in force.
        (for {
          notification <- NotificationPEConverter.toNotification(NotificationFixtures.orphanPE)
          exit         <- ZIO.serviceWithZIO[NotificationRepo](_.insert(notification)).exit
        } yield assert(exit)(fails(anything))).provide(testLayer)
      }
    ),
    suite("find")(
      test("returns Some when the notification exists") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          _      <- NotificationFixtures.seed(ctx = ctx, pe = NotificationFixtures.adaEmailPE)
          result <- ZIO.serviceWithZIO[NotificationRepo](_.find(NotificationFixtures.adaEmailPE.id))
        } yield assert(result.map(_.id))(equalTo(Some(NotificationFixtures.adaEmailPE.id)))).provide(testLayer)
      },
      test("returns None when the notification does not exist") {
        (for {
          result <- ZIO.serviceWithZIO[NotificationRepo](_.find(NotificationId("n-missing")))
        } yield assert(result)(isNone)).provide(testLayer)
      }
    ),
    suite("list")(
      test("returns all seeded notifications") {
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
          result <- ZIO.serviceWithZIO[NotificationRepo](_.list)
          ids = result.map(_.id).toSet
        } yield assert(ids)(
          equalTo(Set(NotificationFixtures.adaEmailPE.id, NotificationFixtures.alanInAppPE.id))
        )).provide(testLayer)
      },
      test("returns empty when the schema is fresh") {
        (for {
          result <- ZIO.serviceWithZIO[NotificationRepo](_.list)
        } yield assert(result)(isEmpty)).provide(testLayer)
      }
    ),
    suite("listForRecipient")(
      test("returns only notifications belonging to the recipient") {
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
          result <- ZIO.serviceWithZIO[NotificationRepo](_.listForRecipient(CustomerFixtures.adaPE.id))
          ids = result.map(_.id).toSet
        } yield assert(ids)(
          equalTo(Set(NotificationFixtures.adaEmailPE.id, NotificationFixtures.adaSmsPE.id))
        )).provide(testLayer)
      },
      test("returns empty when the recipient has no notifications") {
        (for {
          ctx    <- ZIO.service[SqlContext]
          _      <- CustomerFixtures.seed(ctx = ctx, pe = CustomerFixtures.adaPE)
          result <- ZIO.serviceWithZIO[NotificationRepo](_.listForRecipient(CustomerFixtures.adaPE.id))
        } yield assert(result)(isEmpty)).provide(testLayer)
      },
      test("returns empty when the recipient doesn't exist (no existence check)") {
        (for {
          result <- ZIO.serviceWithZIO[NotificationRepo](_.listForRecipient(CustomerId("c-missing")))
        } yield assert(result)(isEmpty)).provide(testLayer)
      }
    )
  )
}
