package org.organization.config

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.auto._
import zio.{Has, ZLayer, ZIO}

final case class HttpServerConfig(host: String, port: Int)

object HttpServerConfig {
  val layer: ZLayer[Any, ConfigReaderFailures, Has[HttpServerConfig]] =
    ZIO
      .fromEither(ConfigSource.default.at("http-server").load[HttpServerConfig])
      .toLayer
}