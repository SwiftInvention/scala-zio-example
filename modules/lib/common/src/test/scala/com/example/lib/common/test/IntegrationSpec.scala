package com.example.lib.common.test

import zio._
import zio.test.{testEnvironment, TestEnvironment, ZIOSpecDefault}

/** Base class for integration specs.
  *
  * Overrides `bootstrap` to install [[TestLogger.layer]] at the spec runtime level. The bootstrap layer is where ZIO's
  * runtime config (including `FiberRef.currentLoggers`) is mutable; an equivalent layer composed via `provideShared`
  * doesn't reach into the runtime's default-logger set, so default loggers leak through.
  *
  * The test logger is silent by default. Set the env var `TEST_LOG_LEVEL` to one of `trace|debug|info|warn|error|fatal`
  * to see logs.
  */
abstract class IntegrationSpec extends ZIOSpecDefault {
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    TestLogger.layer >+> testEnvironment
}
