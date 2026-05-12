package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.common.http.client.AppHttpClient
import com.example.common.http.server.HealthRoutes
import com.example.common.impl.config.{ConfigBootstrap, HttpClientConfig, OtelConfig}
import com.example.common.impl.repo.sql.{DataSourceConfig, DataSourceLayer, SqlContext}
import com.example.common.impl.service.TransactorQuillImpl
import com.example.common.impl.telemetry.AppTracing
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.CustomerApiDirectImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.repo.{AddressRepoMySQLImpl, CustomerRepoMySQLImpl}
import com.example.customer.impl.service.{AddressServiceImpl, CustomerServiceImpl}
import com.example.notification.app.NotificationAppServiceImpl
import com.example.notification.impl.http.NotificationRoutes
import com.example.notification.impl.service.NotificationServiceImpl
import com.example.notification.impl.service.repo.NotificationRepoMySQLImpl
import zio._
import zio.http.Server
import zio.telemetry.opentelemetry.tracing.Tracing

/** Layer composition for the server app. The single place that sees concrete implementations and wires them together.
  *
  * Wiring order: ConfigBootstrap (`EnvLabel`) → typed config slices (DataSourceConfig, OtelConfig, ServerConfig,
  * HttpClientConfig) → DataSourceLayer → SqlContext → Transactor → AppHttpClient → AppTracing → ctx services → zio-http
  * Server.
  *
  * Logging is installed earlier — `ServerApp` overrides `bootstrap` with `AppLogger.bootstrap`, which runs before any
  * layer here builds.
  *
  * Migrations are applied out-of-process (`just db-migrate`).
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
      // ── http client (used by AppTracing's startup probe; available to anyone needing outbound HTTP) ──
      AppHttpClient.layer,
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
      CustomerApiDirectImpl.layer, // cross-ctx contract — consumed by notification's app service
      CustomerRoutes.layer,
      // ── notification ctx ──
      NotificationRepoMySQLImpl.layer,
      NotificationServiceImpl.layer,
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
