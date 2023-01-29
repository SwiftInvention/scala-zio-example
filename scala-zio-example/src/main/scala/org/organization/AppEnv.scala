package org.organization

import com.zaxxer.hikari.HikariConfig
import io.getquill.JdbcContextConfig
import io.getquill.jdbczio.Quill
import io.getquill.util.LoadConfig
import org.organization.config.HttpServerConfig
import zio.Console.printLine
import zio._
import zio.http.Server

import javax.sql.DataSource

object AppEnv {
  type AppEnv = DataSource with Server

  type AppIO[T]     = IO[Throwable, T]
  type AppRIO[R, A] = ZIO[R, Throwable, A]

  private val availableDbSchedule = Schedule
    .fixed(2000.milliseconds)
    .tapOutput(o => printLine(s"Waiting for database to be available, retry count: $o").orDie)

  private val jdbcContextLayer: TaskLayer[JdbcContextConfig] =
    ZLayer {
      ZIO
        .attempt(LoadConfig("mysql"))
        .map(JdbcContextConfig(_))
        .tap(cfg => ZIO.attempt(new HikariConfig(cfg.configProperties).validate()))
    }

  private val dataSourceLayer: RLayer[JdbcContextConfig, DataSource] =
    ZLayer(ZIO.service[JdbcContextConfig])
      .flatMap { env =>
        Quill.DataSource
          .fromJdbcConfig(env.get)
          .retry(availableDbSchedule)
      }

  val buildLiveEnv: TaskLayer[AppEnv] =
    ZLayer.make[AppEnv](
      HttpServerConfig.layer,
      Server.live,
      dataSourceLayer,
      jdbcContextLayer
    )

}
