package org.organization.config

import pureconfig._
import pureconfig.generic.auto._
import zio.http.Server
import zio.{ZIO, ZLayer}

final case class HttpServerConfig(host: String, port: Int)

object HttpServerConfig {
  val layer: ZLayer[Any, Throwable, Server.Config] =
    ZLayer.fromZIO(for {
      conf <- ZIO
        .fromEither(ConfigSource.default.at("http-server").load[HttpServerConfig])
        .mapError(err => new RuntimeException(s"could not get server config due to error: $err"))
    } yield Server.Config.default.binding(hostname = conf.host, port = conf.port))
}
