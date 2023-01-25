package org.organization

import io.getquill.jdbczio.Quill
import org.organization.config.HttpServerConfig
import zio.Console.printLine
import zio._
import zio.http.Server

import javax.sql.DataSource

object AppEnv {
  type AppEnv = DataSource with Server

  type AppIO[T]     = IO[Throwable, T]
  type AppRIO[R, A] = ZIO[R, Throwable, A]

  private lazy val availableDbSchedule = Schedule
    .fixed(2000.milliseconds)
    .tapOutput(o => printLine(s"Waiting for database to be available, retry count: $o").orDie)

  private val dataSourceLayer: ZLayer[Any, Throwable, DataSource] =
    Quill.DataSource.fromPrefix("mysql").retry(availableDbSchedule)

  private val serverLayer: ZLayer[Any, Throwable, Server] =
    HttpServerConfig.layer >>> Server.live

  def buildLiveEnv: ZLayer[Any, Object, AppEnv] =
    serverLayer ++
      dataSourceLayer
}
