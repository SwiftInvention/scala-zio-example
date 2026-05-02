package com.example.app.server

import com.example.customer.impl.http.CustomerRoutes
import zio._
import zio.http.Server

object ServerApp extends ZIOAppDefault {
  def run: ZIO[Any, Throwable, Unit] =
    (ZIO.logInfo("Starting server on http://localhost:8080") *>
      ZIO.serviceWithZIO[CustomerRoutes](r => Server.serve(r.routes)))
      .provide(ServerEnv.layer)
}
