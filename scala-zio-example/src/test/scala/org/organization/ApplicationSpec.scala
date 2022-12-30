package org.organization

import zio.test._

object ApplicationSpec {
  def spec: ZSpec[Any, Nothing] =
    suite("Example unit test suite")(
      test("math works") {
        assertTrue(1 + 1 == 2)
      }
    )
}
