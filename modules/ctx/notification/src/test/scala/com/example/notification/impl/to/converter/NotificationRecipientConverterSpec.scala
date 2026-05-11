package com.example.notification.impl.to.converter

import com.example.notification.domain.model.NotificationGen
import zio.test.Assertion._
import zio.test._

object NotificationRecipientConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("NotificationRecipientConverter")(
    test("toNotificationRecipientTO preserves every field") {
      check(NotificationGen.recipientGen) { r =>
        val to = NotificationRecipientConverter.toNotificationRecipientTO(r)
        assert(to.id)(equalTo(r.id)) &&
        assert(to.email)(equalTo(r.email)) &&
        assert(to.name)(equalTo(r.name))
      }
    }
  )
}
