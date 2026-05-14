package com.example.lib.common.impl.http.client

import com.example.lib.common.impl.config.HttpClientConfig
import zio._
import zio.http.netty.NettyConfig
import zio.http.{Client, DnsResolver, ZClient}

/** Builds the outbound [[zio.http.Client]] layer. Translates typed [[HttpClientConfig]] into zio-http's
  * [[zio.http.ZClient.Config]] and composes that with default `NettyConfig` and `DnsResolver` into `Client.live`.
  * Replace the default providers here if a deployment needs to tune DNS, local-address binding, or SSL.
  */
object AppHttpClient {

  private val configLayer: URLayer[HttpClientConfig, ZClient.Config] =
    ZLayer.fromFunction { (cfg: HttpClientConfig) =>
      ZClient.Config.default
        .connectionTimeout(cfg.connectionTimeout)
        .idleTimeout(cfg.idleTimeout)
    }

  val layer: ZLayer[HttpClientConfig, Throwable, Client] =
    ZLayer.makeSome[HttpClientConfig, Client](
      configLayer,
      ZLayer.succeed(NettyConfig.default),
      DnsResolver.default,
      Client.live
    )
}
