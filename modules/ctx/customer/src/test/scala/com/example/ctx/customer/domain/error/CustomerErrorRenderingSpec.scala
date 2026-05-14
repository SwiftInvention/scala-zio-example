package com.example.ctx.customer.domain.error

import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.impl.http.ErrorTO
import com.example.lib.common.test.SnapshotSpec
import zio.test._

/** Locks the wire shape of `ErrorTO` for each customer-context error.
  *
  * If any error's category, reason, code, or description format changes, the snapshot diff is the alarm. Update the
  * snapshot file deliberately when the change is intended (`SNAPSHOT_UPDATE=true sbt test`).
  */
object CustomerErrorRenderingSpec extends ZIOSpecDefault with SnapshotSpec {

  private val notFound: ErrorTO =
    ErrorTO.from(CustomerNotFoundError.withId(CustomerId("11111111-2222-3333-4444-555555555555")))

  override def spec: Spec[Any, Throwable] = suite("Customer error rendering")(
    test("CustomerNotFoundError → ErrorTO") {
      matchesJsonSnapshot(name = "CustomerErrorRendering/notFound.json", value = notFound)
    }
  )
}
