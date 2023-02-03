package org.organization

import zio.logging.slf4j.bridge.Slf4jBridge
import zio.logging.{LogFilter, LogFormat, LoggerNameExtractor}
import zio.{LogLevel, TaskLayer}

object AppLog {
  private val loggerName =
    LoggerNameExtractor.annotationOrTrace(Slf4jBridge.loggerNameAnnotationKey)

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
      zio.logging.consoleJson(logFormat, logFilter) >+>
      Slf4jBridge.initialize
}
