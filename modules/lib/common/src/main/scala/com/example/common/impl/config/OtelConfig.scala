package com.example.common.impl.config

import com.example.common.domain.error.AppFailure
import com.example.common.domain.model.EnvLabel
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigReader, KebabCase}
import zio._

/** Typed OpenTelemetry config. Read from the `otel` block of the active `application-<env>.conf`.
  *
  *   - `serviceName` — the `service.name` resource attribute on every emitted span. Required.
  *   - `tracing` — [[OtelTracing.Enabled]] (carrying the OTLP HTTP traces URL) or [[OtelTracing.Disabled]]. The HOCON
  *     side stays a nullable string under the key `otlp-endpoint`; a present value parses to `Enabled`, an absent or
  *     `null` value parses to `Disabled`.
  *
  * The ADT makes the binary "tracing on / tracing off" semantic explicit at the consumer site (exhaustive pattern
  * match), where `Option[String]` would conflate "off", "missing key", and "set to null" into one shape.
  *
  * Per `config-shape`: `Disabled` is "tracing genuinely off", not a substitute for a baked-in default.
  */
final case class OtelConfig(
    serviceName: String,
    tracing: OtelTracing
)

sealed trait OtelTracing
object OtelTracing {
  final case class Enabled(otlpEndpoint: String) extends OtelTracing
  case object Disabled                           extends OtelTracing
}

object OtelConfig {

  /** Internal HOCON-shape mirror used only for parsing. Public API exposes [[OtelConfig]] with the ADT.
    *
    * `ProductHint` makes the camelCase → kebab-case mapping explicit at the type, so a future field rename can't
    * silently flip the wire key.
    */
  private final case class Raw(serviceName: String, otlpEndpoint: Option[String])
  private implicit val rawHint: ProductHint[Raw]    = ProductHint[Raw](ConfigFieldMapping(CamelCase, KebabCase))
  private implicit val rawReader: ConfigReader[Raw] = deriveReader[Raw]

  implicit val reader: ConfigReader[OtelConfig] = rawReader.map { raw =>
    // Empty endpoint string (e.g. `OTEL_ENDPOINT=""` set in a deployment without a collector) maps to `Disabled` —
    // an empty URL would otherwise reach the OTLP exporter and fail on every flush.
    val tracing = raw.otlpEndpoint.filter(_.nonEmpty) match {
      case Some(url) => OtelTracing.Enabled(url)
      case None      => OtelTracing.Disabled
    }
    OtelConfig(serviceName = raw.serviceName, tracing = tracing)
  }

  val layer: ZLayer[EnvLabel, AppFailure, OtelConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[OtelConfig]("otel"))
}
