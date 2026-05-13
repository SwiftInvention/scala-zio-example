package com.example.app.server.config

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.model.EnvLabel
import com.example.lib.common.impl.config.ConfigBootstrap
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio._

/** Typed HTTP server config. Read from the `server` block of the active `application-<env>.conf`. */
final case class ServerConfig(
    host: String,
    port: Int
)

object ServerConfig {
  implicit val reader: ConfigReader[ServerConfig] = deriveReader[ServerConfig]

  val layer: ZLayer[EnvLabel, AppFailure, ServerConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[ServerConfig]("server"))
}
