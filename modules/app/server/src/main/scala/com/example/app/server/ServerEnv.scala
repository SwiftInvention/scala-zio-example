package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.ctx.customer.app.CustomerAppServiceImpl
import com.example.ctx.customer.impl.CustomerApiDirectImpl
import com.example.ctx.customer.impl.http.CustomerRoutes
import com.example.ctx.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.ctx.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import com.example.ctx.notification.app.NotificationAppServiceImpl
import com.example.ctx.notification.impl.http.NotificationRoutes
import com.example.ctx.notification.impl.service.repo.NotificationRepoMySQLImpl
import com.example.lib.common.impl.config.{ConfigBootstrap, HttpClientConfig, OtelConfig}
import com.example.lib.common.impl.http.client.AppHttpClient
import com.example.lib.common.impl.http.server.HealthRoutes
import com.example.lib.common.impl.telemetry.AppTracing
import com.example.lib.db.impl.service.{DbProbeQuillImpl, TransactorQuillImpl}
import com.example.lib.db.impl.sql.{DataSourceConfig, DataSourceLayer, SqlContext}
import zio._
import zio.http.Server
import zio.telemetry.opentelemetry.tracing.Tracing

/** Layer composition for the server app. The single place that sees concrete implementations and wires them together.
  * Logging is installed by `ServerApp.bootstrap` before this layer builds; migrations are applied out-of-process via
  * `just db-migrate`.
  */
object ServerEnv {
  type AppEnv = ServerRoutes & Server & ServerConfig & Tracing

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
      HttpClientConfig.layer,
      OtelConfig.layer,
      ServerConfig.layer,
      // ── http client (used by AppTracing's startup probe) ──
      AppHttpClient.layer,
      // ── tracing (OTLP HTTP exporter when endpoint set; no-op otherwise) ──
      AppTracing.live,
      // ── persistence ──
      DataSourceLayer.layer,
      SqlContext.layer,
      TransactorQuillImpl.layer,
      DbProbeQuillImpl.layer,
      // ── customer ctx ──
      CustomerRepoMySQLImpl.layer,
      AddressRepoMySQLImpl.layer,
      CustomerServiceImpl.layer,
      AddressServiceImpl.layer,
      CustomerAppServiceImpl.layer,
      CustomerApiDirectImpl.layer, // cross-ctx contract — consumed by notification's app service
      CustomerRoutes.layer,
      // ── notification ctx ──
      NotificationRepoMySQLImpl.layer,
      NotificationAppServiceImpl.layer,
      NotificationRoutes.layer,
      // ── operational ──
      HealthRoutes.layer,
      // ── route composition ──
      ServerRoutes.layer,
      // ── http ──
      httpServerLayer
    )
}
