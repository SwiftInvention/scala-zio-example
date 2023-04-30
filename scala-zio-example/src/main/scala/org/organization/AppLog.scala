package org.organization

import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{ConsoleLoggerConfig, LogFilter, LogFormat, LoggerNameExtractor}
import zio.{LogLevel, TaskLayer}

object AppLog {
  private val loggerName =
    LoggerNameExtractor.annotationOrTrace(zio.logging.loggerNameAnnotationKey)

  private val logFormat =
    LogFormat.label("name", loggerName.toLogFormat()) + LogFormat.default

  private val logFilter =
    LogFilter.logLevelByGroup(
      LogLevel.Info,
      loggerName.toLogGroup(),
      "org.organization" -> LogLevel.Debug
    )

  val live: TaskLayer[Unit] =
    zio.Runtime.removeDefaultLoggers >>>
      zio.logging.consoleJsonLogger(
        ConsoleLoggerConfig(
          format = logFormat,
          filter = logFilter
        )
      ) >+>
      Slf4jBridge.initialize
}
