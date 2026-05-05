package com.example.app.server.config

import com.example.common.domain.model.EnvLabel
import com.example.common.impl.config.ConfigBootstrap
import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import zio._

/** Typed HTTP server config. Read from the `server` block of the active `application-<env>.conf`. No defaults on the
  * case class.
  */
final case class ServerConfig(
    host: String,
    port: Int
)

object ServerConfig {
  implicit val reader: ConfigReader[ServerConfig] = deriveReader[ServerConfig]

  val layer: ZLayer[EnvLabel, Throwable, ServerConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[ServerConfig]("server"))
}
