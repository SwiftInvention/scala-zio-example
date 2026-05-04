package com.example.app.server

import com.example.app.server.config.ServerConfig
import com.example.customer.impl.http.CustomerRoutes
import zio._
import zio.http.Server

object ServerApp extends ZIOAppDefault {
  def run: ZIO[Any, Throwable, Unit] = {
    val serve =
      ZIO.service[ServerConfig].flatMap { cfg =>
        ZIO.logInfo(s"Starting server on http://${cfg.host}:${cfg.port}") *>
          ZIO.serviceWithZIO[CustomerRoutes](r => Server.serve(r.routes))
      }

    serve.provide(ServerEnv.layer)
  }
}
