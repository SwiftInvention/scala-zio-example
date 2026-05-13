package com.example.lib.db.impl.sql

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.model.EnvLabel
import com.example.lib.common.impl.config.ConfigBootstrap
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio._

/** Typed datasource config. Read from the `database.data-source` block of the active `application-<env>.conf`. */
final case class DataSourceConfig(
    jdbcUrl: String,
    user: String,
    password: String,
    maximumPoolSize: Int
)

object DataSourceConfig {
  implicit val reader: ConfigReader[DataSourceConfig] = deriveReader[DataSourceConfig]

  val layer: ZLayer[EnvLabel, AppFailure, DataSourceConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[DataSourceConfig]("database.data-source"))
}
