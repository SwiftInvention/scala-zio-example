package com.example.common.impl.logging

import com.example.common.domain.error.AppFailure
import com.example.common.domain.model.LogFormatKind
import com.example.common.impl.config.{ConfigBootstrap, LoggingConfig}
import zio._
import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{consoleJsonLogger, consoleLogger, ConsoleLoggerConfig, LogFilter, LogFormat}

/** Boot-time logger setup.
  *
  * `bootstrap` is intended to be the `bootstrap` layer of every `ZIOAppDefault` entrypoint (`ServerApp`, `Experiment`,
  * each action under `actions/`). `ZIOAppDefault` runs its bootstrap before any layer in the main `run` effect builds,
  * so the configured logger replaces ZIO's defaults before the first user-level log line emits — including
  * `ConfigBootstrap.layer`'s `APP_ENV resolved: ...` line that fires when the main layer chain starts.
  *
  * `bootstrap` does its own minimal config load (APP_ENV → `LoggingConfig`) instead of consuming
  * `ConfigBootstrap.layer` and `LoggingConfig.layer`. Those layers can't run yet — they're part of the env that the
  * configured logger is installed before.
  */
object AppLogger {

  /** Pretty (dev) format: colored timestamp/level/fiber/line, then any annotations as `key=value` after the message. */
  private val prettyFormat: LogFormat =
    LogFormat.colored + LogFormat.allAnnotations

  /** JSON (deployed) format: same field set, rendered as a single-line JSON object by `consoleJsonLogger`. Annotations
    * become top-level keys.
    */
  private val jsonFormat: LogFormat =
    LogFormat.default + LogFormat.allAnnotations

  /** Install the configured console logger and slf4j bridge. Reads APP_ENV and the `logging` block of the active
    * `application-<env>.conf` directly. Fails fast on missing/invalid APP_ENV or malformed config — the app refuses to
    * start.
    *
    * If the bootstrap itself fails (bad APP_ENV, malformed `logging` block), the resulting `AppFailure` renders through
    * ZIO's stock cause printer rather than the configured pretty/JSON formatter — the configured logger isn't installed
    * yet at that point.
    */
  val bootstrap: ZLayer[Any, AppFailure, Unit] = {
    val loadCfg: IO[AppFailure, LoggingConfig] =
      ConfigBootstrap.readEnvLabel.flatMap { envLabel =>
        ConfigBootstrap.load[LoggingConfig]("logging").provideEnvironment(ZEnvironment(envLabel))
      }

    ZLayer.fromZIO(loadCfg).flatMap { env =>
      val cfg    = env.get
      val filter = LogFilter.LogLevelByNameConfig(cfg.level)
      val install: ZLayer[Any, Nothing, Unit] = cfg.format match {
        case LogFormatKind.Pretty =>
          Runtime.removeDefaultLoggers >>> consoleLogger(ConsoleLoggerConfig(prettyFormat, filter))
        case LogFormatKind.Json =>
          Runtime.removeDefaultLoggers >>> consoleJsonLogger(ConsoleLoggerConfig(jsonFormat, filter))
      }
      install >+> Slf4jBridge.initialize
    }
  }

}
