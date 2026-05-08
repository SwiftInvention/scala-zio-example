package com.example.common.impl.repo.sql

import com.example.common.domain.error.AppFailure
import com.example.common.domain.model.EnvLabel
import com.example.common.impl.config.ConfigBootstrap
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio._

/** Typed datasource config. Read from the `database.data-source` block of the active `application-<env>.conf`.
  *
  * No defaults on the case class — fields are required. PureConfig fails-fast if any are missing. See the
  * `config-shape` principle.
  */
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
