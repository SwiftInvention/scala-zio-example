package com.example.lib.db.impl.sql

import javax.sql.DataSource

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.error.backend.DbError
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zio._

/** Hikari `DataSource` constructed from a typed `DataSourceConfig`.
  *
  * Lifecycle: scoped layer — pool is closed on app shutdown.
  */
object DataSourceLayer {

  val layer: ZLayer[DataSourceConfig, AppFailure, DataSource] =
    ZLayer.scoped {
      ZIO.serviceWithZIO[DataSourceConfig] { cfg =>
        val acquire: IO[AppFailure, HikariDataSource] =
          ZIO
            .attempt {
              val hc = new HikariConfig()
              hc.setJdbcUrl(cfg.jdbcUrl)
              hc.setUsername(cfg.user)
              hc.setPassword(cfg.password)
              hc.setMaximumPoolSize(cfg.maximumPoolSize)
              hc.validate()
              new HikariDataSource(hc)
            }
            .mapError(e => DbError(s"Failed to initialize datasource: ${e.getMessage}", Some(e)))

        val release = (ds: HikariDataSource) =>
          ZIO
            .attempt(ds.close())
            .tapErrorCause(c => ZIO.logErrorCause("Failed to close datasource", c))
            .ignore

        ZIO.acquireRelease(acquire)(release).map(identity[DataSource])
      }
    }
}
