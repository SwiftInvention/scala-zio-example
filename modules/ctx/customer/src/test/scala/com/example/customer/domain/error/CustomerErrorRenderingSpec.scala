package com.example.customer.domain.error

import com.example.common.domain.error.api.ErrorTO
import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.test.SnapshotSpec
import zio.test._

/** Locks the wire shape of `ErrorTO` for each customer-context error.
  *
  * If any error's category, reason, code, or description format changes, the snapshot diff is the alarm. Update the
  * snapshot file deliberately when the change is intended (`SNAPSHOT_UPDATE=true sbt test`).
  */
object CustomerErrorRenderingSpec extends ZIOSpecDefault with SnapshotSpec {

  private val notFound: ErrorTO =
    ErrorTO.from(CustomerNotFoundError.withId(CustomerId("11111111-2222-3333-4444-555555555555")))

  private val invalidEmail: ErrorTO =
    ErrorTO.from(InvalidEmailError(message = "Invalid email: 'not-an-email'"))

  private val invalidName: ErrorTO =
    ErrorTO.from(InvalidCustomerNameError(message = "Customer name must not be empty"))

  override def spec: Spec[Any, Throwable] = suite("Customer error rendering")(
    test("CustomerNotFoundError → ErrorTO") {
      matchesJsonSnapshot(name = "CustomerErrorRendering/notFound.json", value = notFound)
    },
    test("InvalidEmailError → ErrorTO") {
      matchesJsonSnapshot(name = "CustomerErrorRendering/invalidEmail.json", value = invalidEmail)
    },
    test("InvalidCustomerNameError → ErrorTO") {
      matchesJsonSnapshot(name = "CustomerErrorRendering/invalidName.json", value = invalidName)
    }
  )
}
