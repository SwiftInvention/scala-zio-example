package com.example.lib.common.impl.config

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.model.{EnvLabel, URLHelper}
import pureconfig.error.CannotConvert
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, KebabCase}
import zio._
import zio.http.URL

/** Typed OpenTelemetry config. Read from the `otel` block of the active `application-<env>.conf`.
  *
  *   - `serviceName` — the `service.name` resource attribute on every emitted span. Required.
  *   - `tracing` — [[OtelTracing.Enabled]] (carrying the OTLP HTTP traces URL as a `zio.http.URL`) or
  *     [[OtelTracing.Disabled]]. The HOCON side stays a nullable string under the key `otlp-endpoint`; a present
  *     well-formed URL parses to `Enabled`, an absent or `null` value parses to `Disabled`. An empty string and a
  *     malformed URL each fail the reader with `CannotConvert`.
  *
  * The ADT makes the binary "tracing on / tracing off" semantic explicit at the consumer site (exhaustive pattern
  * match), where `Option[String]` would conflate "off", "missing key", and "set to null" into one shape.
  *
  * Empty string is treated as a config error rather than a synonym for "off": deployments that wire `otlp-endpoint =
  * ${OTEL_ENDPOINT}` and forget to set `OTEL_ENDPOINT` get a loud failure at parse time. Deployments that want tracing
  * off omit the key or set it to `null`.
  */
final case class OtelConfig(
    serviceName: String,
    tracing: OtelTracing
)

sealed trait OtelTracing
object OtelTracing {
  final case class Enabled(otlpEndpoint: URL) extends OtelTracing
  case object Disabled                        extends OtelTracing
}

object OtelConfig {

  /** Internal HOCON-shape mirror used only for parsing. Public API exposes [[OtelConfig]] with the ADT. */
  private final case class Raw(serviceName: String, otlpEndpoint: Option[String])
  private implicit val rawHint: ProductHint[Raw]    = ProductHint[Raw](ConfigFieldMapping(CamelCase, KebabCase))
  private implicit val rawReader: ConfigReader[Raw] = deriveReader[Raw]

  implicit val reader: ConfigReader[OtelConfig] = rawReader.emap { raw =>
    raw.otlpEndpoint match {
      case Some("") =>
        Left(
          CannotConvert(
            value = "",
            toType = "URL",
            because = "otlp-endpoint must be absent, null, or a non-empty URL — an empty string is not a valid endpoint"
          )
        )
      case Some(s) =>
        URLHelper.parseEither(s) match {
          case Right(url) =>
            Right(OtelConfig(serviceName = raw.serviceName, tracing = OtelTracing.Enabled(url)))
          case Left(invalid) =>
            Left(
              CannotConvert(
                value = s,
                toType = "URL",
                because = s"otlp-endpoint is not a well-formed URL: ${invalid.message}"
              )
            )
        }
      case None => Right(OtelConfig(serviceName = raw.serviceName, tracing = OtelTracing.Disabled))
    }
  }

  val layer: ZLayer[EnvLabel, AppFailure, OtelConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[OtelConfig]("otel"))
}
