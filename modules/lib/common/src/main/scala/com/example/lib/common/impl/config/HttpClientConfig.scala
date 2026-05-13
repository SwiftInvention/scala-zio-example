package com.example.lib.common.impl.config

import java.time.Duration

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.model.EnvLabel
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, KebabCase}
import zio._

/** Typed outbound HTTP client config. Read from the `http-client` block of the active `application-<env>.conf`.
  *
  *   - `connectionTimeout` — TCP connect timeout per request. Bounds time spent on an unreachable host.
  *   - `idleTimeout` — how long an idle pooled connection survives before close.
  *
  * Per-request timeouts (full response read) belong on the caller via `effect.timeoutFail(...)(...)` — they vary by
  * endpoint and don't belong in a shared client config.
  */
final case class HttpClientConfig(
    connectionTimeout: Duration,
    idleTimeout: Duration
)

object HttpClientConfig {

  // `java.time.Duration` is what zio-http's `Client.Config` consumes. PureConfig parses HOCON `5s`, `100ms`, etc.
  private implicit val hint: ProductHint[HttpClientConfig] =
    ProductHint[HttpClientConfig](ConfigFieldMapping(CamelCase, KebabCase))

  implicit val reader: ConfigReader[HttpClientConfig] = deriveReader[HttpClientConfig]

  val layer: ZLayer[EnvLabel, AppFailure, HttpClientConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[HttpClientConfig]("http-client"))
}
