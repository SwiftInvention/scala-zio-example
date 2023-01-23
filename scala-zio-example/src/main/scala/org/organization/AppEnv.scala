package org.organization

import io.getquill.context.ZioJdbc.DataSourceLayer
import org.organization.config.HttpServerConfig
import zhttp.service.EventLoopGroup
import zhttp.service.server.ServerChannelFactory
import zio._
import zio.Clock
import zio.duration.durationInt

import javax.sql.DataSource
import zio.Console
import zio.Console.printLine

object AppEnv {
  type AppEnv = HttpServerConfig
    with Clock.Service
    with Console
    with DataSource
    with EventLoopGroup
    with zhttp.service.ServerChannelFactory

  type AppIO[T]     = IO[Throwable, T]
  type AppRIO[R, A] = ZIO[R, Throwable, A]

  private lazy val availableDbSchedule = Schedule
    .fixed(2000.milliseconds)
    .tapOutput(o => printLine(s"Waiting for database to be available, retry count: $o").orDie)

  def buildLiveEnv: ZLayer[Console with Clock, Object, AppEnv] =
    HttpServerConfig.layer ++ Clock.live ++ Console.live ++
      DataSourceLayer.fromPrefix("mysql").retry(availableDbSchedule) ++
      EventLoopGroup.auto(0) ++ ServerChannelFactory.auto
}
