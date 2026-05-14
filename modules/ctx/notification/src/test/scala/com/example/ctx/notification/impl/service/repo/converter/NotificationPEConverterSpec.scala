package com.example.ctx.notification.impl.service.repo.converter

import com.example.ctx.notification.domain.model.NotificationGen
import zio.test.Assertion._
import zio.test._

object NotificationPEConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("NotificationPEConverter")(
    test("toNotification ∘ toNotificationPE preserves identity (round-trip law)") {
      check(NotificationGen.notificationGen) { n =>
        for {
          r <- NotificationPEConverter.toNotification(NotificationPEConverter.toNotificationPE(n))
        } yield assert(r)(equalTo(n))
      }
    }
  )
}
