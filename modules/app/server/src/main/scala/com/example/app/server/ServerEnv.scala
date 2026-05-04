package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.common.impl.config.ConfigBootstrap
import com.example.common.impl.repo.pg.{DataSourceConfig, DataSourceLayer, PgContext}
import com.example.common.impl.service.TransactorQuillImpl
import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.CustomerApiDirectImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.CustomerServiceImpl
import com.example.customer.impl.service.repo.CustomerRepoMySQLImpl
import zio._
import zio.http.Server

/** Layer composition for the server app. The single place that sees concrete
  * implementations and wires them together.
  *
  * Wiring order:
  *   ConfigBootstrap (`EnvConfig`)
  *     → DataSourceConfig + ServerConfig (typed slices)
  *       → DataSourceLayer → PgContext → Transactor → CustomerRepoMySQLImpl
  *       → zio-http Server (binding from ServerConfig)
  *
  * Migrations are applied out-of-process (`just db-migrate`).
  */
object ServerEnv {
  type AppEnv = CustomerRoutes & Server & ServerConfig

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
      ServerConfig.layer,
      // ── persistence ──
      DataSourceLayer.layer,
      PgContext.layer,
      TransactorQuillImpl.layer,
      // ── customer ctx ──
      CustomerRepoMySQLImpl.layer,
      CustomerServiceImpl.layer,
      CustomerAppServiceImpl.layer,
      CustomerApiDirectImpl.layer,
      CustomerRoutes.layer,
      // ── http ──
      httpServerLayer
    )
}
