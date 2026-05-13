package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.lib.common.impl.logging.AppLogger
import zio._
import zio.http.Server

object ServerApp extends ZIOAppDefault {

  /** Install the configured logger before any layer in the main `run` effect builds. This makes
    * `ConfigBootstrap.layer`'s `APP_ENV resolved` log — and every log after — use the configured format.
    */
  override val bootstrap: ZLayer[Any, Any, Any] = AppLogger.bootstrap

  def run: ZIO[Any, Throwable, Unit] = {
    val acquire =
      for {
        cfg <- ZIO.service[ServerConfig]
        sr  <- ZIO.service[ServerRoutes]
        _   <- ZIO.logInfo(s"Starting server on http://${cfg.host}:${cfg.port}")
      } yield sr

    val serve = acquire.flatMap(sr => Server.serve(sr.all))

    // Top-level cause log — last-resort visibility before the runtime swallows. Per the `logging` principle:
    // every boundary where an error could be lost gets a log; this is the outermost one.
    serve
      .tapErrorCause(cause => ZIO.logErrorCause("Server crashed", cause))
      .provide(ServerEnv.layer)
  }
}
