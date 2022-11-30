package org.organization

import org.organization.config.HttpServerConfig
import io.getquill.context.ZioJdbc.DataSourceLayer

import javax.sql.DataSource
import zhttp.service.EventLoopGroup
import zhttp.service.server.ServerChannelFactory
import zio._
import zio.clock.Clock
import zio.duration.durationInt
import zio.console.{Console, putStrLn}

object AppEnv {
  type AppEnv = Has[HttpServerConfig]
    with Has[Clock.Service]
    with Has[Console.Service]
    with Has[DataSource]
    with EventLoopGroup
    with zhttp.service.ServerChannelFactory

  type AppIO[T] = ZIO[AppEnv, Throwable, T]

  lazy val availableDbSchedule = Schedule
    .fixed(2000.milliseconds)
    .tapOutput(o => putStrLn(s"Waiting for database to be available, retry count: $o").orDie)

  def buildLiveEnv =
    HttpServerConfig.layer ++ Clock.live ++ Console.live ++
      DataSourceLayer.fromPrefix("mysql").retry(availableDbSchedule) ++
      EventLoopGroup.auto(0) ++ ServerChannelFactory.auto
}
