package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.common.impl.config.{ConfigBootstrap, OtelConfig}
import com.example.common.impl.repo.sql.{DataSourceConfig, DataSourceLayer, SqlContext}
import com.example.common.impl.service.TransactorQuillImpl
import com.example.common.impl.telemetry.AppTracing
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import zio._
import zio.http.Server
import zio.telemetry.opentelemetry.tracing.Tracing

/** Layer composition for the server app. The single place that sees concrete implementations and wires them together.
  *
  * Wiring order: ConfigBootstrap (`EnvLabel`) → typed config slices (DataSourceConfig, OtelConfig, ServerConfig) →
  * DataSourceLayer → SqlContext → Transactor → AppTracing → ctx services → zio-http Server.
  *
  * Logging is installed earlier — `ServerApp` overrides `bootstrap` with `AppLogger.bootstrap`, which runs before any
  * layer here builds.
  *
  * Migrations are applied out-of-process (`just db-migrate`).
  */
object ServerEnv {
  type AppEnv = CustomerRoutes & Server & ServerConfig & Tracing

  /** Translates `ServerConfig` to zio-http's `Server.Config` and produces a `Server`. */
  private val httpServerLayer: ZLayer[ServerConfig, Throwable, Server] = {
    val httpCfgLayer: URLayer[ServerConfig, Server.Config] =
      ZLayer.fromFunction((sc: ServerConfig) => Server.Config.default.binding(sc.host, sc.port))
    httpCfgLayer >>> Server.live
  }

  val layer: ZLayer[Any, Throwable, AppEnv] =
    ZLayer.make[AppEnv](
      // ── config ──
      ConfigBootstrap.layer,
      DataSourceConfig.layer,
      OtelConfig.layer,
      ServerConfig.layer,
      // ── tracing (OTLP HTTP exporter when endpoint set; no-op otherwise) ──
      AppTracing.live,
      // ── persistence ──
      DataSourceLayer.layer,
      SqlContext.layer,
      TransactorQuillImpl.layer,
      // ── customer ctx ──
      CustomerRepoMySQLImpl.layer,
      AddressRepoMySQLImpl.layer,
      CustomerServiceImpl.layer,
      AddressServiceImpl.layer,
      CustomerAppServiceImpl.layer,
      CustomerRoutes.layer,
      // ── http ──
      httpServerLayer
    )
}
