package com.example.ctx.customer.domain.error

import com.example.lib.common.domain.model.NewTypes.AddressId
import com.example.lib.common.impl.http.ErrorTO
import com.example.lib.common.test.SnapshotSpec
import zio.test._

/** Locks the wire shape of `ErrorTO` for each address-related error. */
object AddressErrorRenderingSpec extends ZIOSpecDefault with SnapshotSpec {

  private val notFound: ErrorTO =
    ErrorTO.from(AddressNotFoundError.withId(AddressId("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")))

  private val invalidLine: ErrorTO =
    ErrorTO.from(InvalidAddressLineError(message = "Address line must not be empty"))

  private val invalidCity: ErrorTO =
    ErrorTO.from(InvalidCityError(message = "City must not be empty"))

  private val invalidPostalCode: ErrorTO =
    ErrorTO.from(InvalidPostalCodeError(message = "Postal code must not be empty"))

  override def spec: Spec[Any, Throwable] = suite("Address error rendering")(
    test("AddressNotFoundError → ErrorTO") {
      matchesJsonSnapshot(name = "AddressErrorRendering/notFound.json", value = notFound)
    },
    test("InvalidAddressLineError → ErrorTO") {
      matchesJsonSnapshot(name = "AddressErrorRendering/invalidLine.json", value = invalidLine)
    },
    test("InvalidCityError → ErrorTO") {
      matchesJsonSnapshot(name = "AddressErrorRendering/invalidCity.json", value = invalidCity)
    },
    test("InvalidPostalCodeError → ErrorTO") {
      matchesJsonSnapshot(name = "AddressErrorRendering/invalidPostalCode.json", value = invalidPostalCode)
    }
  )
}
