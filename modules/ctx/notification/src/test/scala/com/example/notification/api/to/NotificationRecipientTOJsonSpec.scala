package com.example.notification.api.to

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.test.SnapshotSpec
import zio.test._

object NotificationRecipientTOJsonSpec extends ZIOSpecDefault with SnapshotSpec {

  private val one = NotificationRecipientTO(
    id = CustomerId("c-11111111-2222-3333-4444-555555555555"),
    email = "ada@example.test",
    name = "Ada Lovelace"
  )

  override def spec: Spec[Any, Throwable] = suite("NotificationRecipientTO JSON wire format")(
    test("single recipient matches snapshot") {
      matchesJsonSnapshot(name = "NotificationRecipientTO/single.json", value = one)
    }
  )
}
