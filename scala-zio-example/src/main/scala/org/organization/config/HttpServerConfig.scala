package org.organization.config

import pureconfig._
import pureconfig.generic.auto._
import zio.http.ServerConfig
import zio.{ZIO, ZLayer}

final case class HttpServerConfig(host: String, port: Int)

object HttpServerConfig {
  val layer: ZLayer[Any, Throwable, ServerConfig] =
    ZLayer.fromZIO(for {
      conf <- ZIO
        .fromEither(ConfigSource.default.at("http-server").load[HttpServerConfig])
        .mapError(err => new RuntimeException(s"could not get server config due to error: $err"))
    } yield ServerConfig.default.binding(hostname = conf.host, port = conf.port))
}
