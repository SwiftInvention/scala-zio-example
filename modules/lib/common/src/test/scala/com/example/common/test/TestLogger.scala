package com.example.common.test

import zio._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{consoleLogger, ConsoleLoggerConfig, LogFilter, LogFormat}

/** Logger setup for test runs. Test-only; lives next to the test fixtures rather than alongside the production
  * `AppLogger` so live code doesn't surface a test-specific API.
  *
  * Behavior of [[layer]]:
  *
  *   - Silent by default — strips ZIO's default loggers, installs no replacement, bridges slf4j (so library logs from
  *     code under test go through the empty logger set and drop silently).
  *   - Override by setting the env var `TEST_LOG_LEVEL` to one of `trace|debug|info|warn|error|fatal`. The configured
  *     level installs a pretty console logger; useful for debugging a flaky spec.
  *
  * Designed to be installed at spec `bootstrap` — see [[IntegrationSpec]]. Installing it via `provideShared` doesn't
  * reach the test runtime's default-logger set, so default loggers leak through and emit log lines from inside the code
  * under test.
  *
  * sbt's forked test JVM inherits the parent process's environment by default, so `TEST_LOG_LEVEL=debug sbt
  * appIntegrationTests/test` (or per-spec `sbt "appIntegrationTests/testOnly *SpecName"`) propagates the value into the
  * running test JVM without extra plumbing.
  */
object TestLogger {

  private val prettyFormat: LogFormat = LogFormat.colored + LogFormat.allAnnotations

  val layer: ZLayer[Any, Nothing, Unit] = {
    val overrideLevel: Option[LogLevel] =
      sys.env.get("TEST_LOG_LEVEL").map(_.trim.toUpperCase).flatMap {
        case "TRACE"            => Some(LogLevel.Trace)
        case "DEBUG"            => Some(LogLevel.Debug)
        case "INFO"             => Some(LogLevel.Info)
        case "WARN" | "WARNING" => Some(LogLevel.Warning)
        case "ERROR"            => Some(LogLevel.Error)
        case "FATAL"            => Some(LogLevel.Fatal)
        case _                  => None
      }

    val baseLogger: ZLayer[Any, Nothing, Unit] = overrideLevel match {
      case Some(level) =>
        Runtime.removeDefaultLoggers >>>
          consoleLogger(ConsoleLoggerConfig(prettyFormat, LogFilter.LogLevelByNameConfig(level)))
      case None =>
        Runtime.removeDefaultLoggers
    }

    baseLogger >+> Slf4jBridge.initialize
  }
}
