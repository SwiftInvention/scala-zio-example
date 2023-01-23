package org.organization

import zio.test.Assertion.equalTo
import zio.test._
import zio.test.ZIOSpecDefault

object ApplicationSpec extends ZIOSpecDefault {
  def spec: ZSpec[Any, Nothing] =
    suite("Example unit test suite")(
      test("math works") {
        assert(1 + 1)(equalTo(2))
      }
    )
}
