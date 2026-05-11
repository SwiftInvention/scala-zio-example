package com.example.notification.impl.to.converter

import com.example.notification.domain.model.NotificationGen
import zio.test.Assertion._
import zio.test._

object NotificationConverterSpec extends ZIOSpecDefault {

  override def spec: Spec[Any, Throwable] = suite("NotificationConverter")(
    test("toNotificationTO preserves every field") {
      check(NotificationGen.notificationGen) { n =>
        val to = NotificationConverter.toNotificationTO(n)
        assert(to.id)(equalTo(n.id)) &&
        assert(to.recipientId)(equalTo(n.recipientId)) &&
        assert(to.channel)(equalTo(n.channel.entryName)) &&
        assert(to.message)(equalTo(n.message.value)) &&
        assert(to.createdAt)(equalTo(n.createdAt))
      }
    }
  )
}
