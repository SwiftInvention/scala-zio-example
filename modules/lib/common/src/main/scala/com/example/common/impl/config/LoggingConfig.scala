package com.example.common.impl.config

import com.example.common.domain.model.LogFormatKind
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.semiauto.deriveReader
import zio._

/** Typed logging config. Read from the `logging` block of the active `application-<env>.conf`.
  *
  *   - `format` — `pretty` (dev) or `json` (deployed). Drives [[com.example.common.impl.logging.AppLogger]].
  *   - `level` — global minimum log level. Records below this aren't emitted.
  *
  * No defaults on the case class — fields are required per the `config-shape` principle.
  *
  * No `LoggingConfig.layer`: logging is installed at `bootstrap` time (see `AppLogger.bootstrap`), before any layer in
  * the main env builds. `AppLogger.bootstrap` calls `ConfigBootstrap.load[LoggingConfig]("logging")` directly.
  */
final case class LoggingConfig(
    format: LogFormatKind,
    level: LogLevel
)

object LoggingConfig {

  /** Custom reader: `zio.LogLevel` is a sealed value, not a derivable case class. Map a string literal so config files
    * stay declarative.
    */
  implicit val logLevelReader: ConfigReader[LogLevel] =
    ConfigReader.fromString { s =>
      s.trim.toUpperCase match {
        case "TRACE"            => Right(LogLevel.Trace)
        case "DEBUG"            => Right(LogLevel.Debug)
        case "INFO"             => Right(LogLevel.Info)
        case "WARN" | "WARNING" => Right(LogLevel.Warning)
        case "ERROR"            => Right(LogLevel.Error)
        case "FATAL"            => Right(LogLevel.Fatal)
        case other              => Left(CannotConvert(other, "LogLevel", "valid: trace|debug|info|warn|error|fatal"))
      }
    }

  implicit val reader: ConfigReader[LoggingConfig] = deriveReader[LoggingConfig]
}
