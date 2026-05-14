package com.example.lib.common.domain.error.domain

import com.example.lib.common.impl.http.ErrorTO
import com.example.lib.common.test.SnapshotSpec
import zio.test._

/** Locks the wire shape of `ErrorTO` for each `DomainError`. If the category, reason, code, or description format
  * changes, the snapshot diff is the alarm. Update the snapshot file deliberately when the change is intended
  * (`SNAPSHOT_UPDATE=true sbt test`).
  */
object DomainErrorRenderingSpec extends ZIOSpecDefault with SnapshotSpec {

  private val invalidEmail: ErrorTO =
    ErrorTO.from(InvalidEmailError(message = "Invalid email: 'not-an-email'"))

  private val invalidCustomerName: ErrorTO =
    ErrorTO.from(InvalidCustomerNameError(message = "Customer name must not be empty"))

  private val invalidURL: ErrorTO =
    ErrorTO.from(InvalidURLError(message = "Invalid URL 'jdbc:mysql://x': MalformedURLException"))

  override def spec: Spec[Any, Throwable] = suite("Domain error rendering")(
    test("InvalidEmailError → ErrorTO") {
      matchesJsonSnapshot(name = "DomainErrorRendering/invalidEmail.json", value = invalidEmail)
    },
    test("InvalidCustomerNameError → ErrorTO") {
      matchesJsonSnapshot(name = "DomainErrorRendering/invalidCustomerName.json", value = invalidCustomerName)
    },
    test("InvalidURLError → ErrorTO") {
      matchesJsonSnapshot(name = "DomainErrorRendering/invalidURL.json", value = invalidURL)
    }
  )
}
