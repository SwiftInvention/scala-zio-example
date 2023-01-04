package org.organization

import zio.test.Assertion.equalTo
import zio.test._

object ApplicationSpec extends DefaultRunnableSpec {
  def spec: ZSpec[Any, Nothing] =
    suite("Example unit test suite")(
      test("math works") {
        assert(1 + 1)(equalTo(2))
      }
    )
}
