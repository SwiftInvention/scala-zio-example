package org.organization

import cats.implicits.catsSyntaxEq
import zio.test.{ZIOSpecDefault, _}

object ApplicationSpec extends ZIOSpecDefault {
  def spec: Spec[Any, Nothing] =
    suite("Example unit test suite")(
      test("math works") {
        assertTrue(1 + 1 === 2)
      }
    )
}
