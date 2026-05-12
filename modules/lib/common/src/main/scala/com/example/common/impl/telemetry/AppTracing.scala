package com.example.common.impl.telemetry

import com.example.common.impl.config.{OtelConfig, OtelTracing}
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.{OpenTelemetry => JOpenTelemetry}
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import zio._
import zio.telemetry.opentelemetry.OpenTelemetry
import zio.telemetry.opentelemetry.tracing.Tracing

/** Builds a `Tracing` service from `OtelConfig`.
  *
  *   - For `OtelTracing.Enabled`, constructs a real `OpenTelemetrySdk` with an OTLP HTTP exporter, a
  *     `BatchSpanProcessor`, and `service.name` set from `serviceName`. All builders are wrapped in
  *     `ZIO.fromAutoCloseable` so `BatchSpanProcessor.close()` flushes the in-flight batch when the app's scope ends.
  *   - For `OtelTracing.Disabled`, uses `OpenTelemetry.noop`. Span calls become cheap no-ops; consumers stay
  *     unconditional (no `if (tracingEnabled) tracing.span(...) else effect`).
  *
  * Span context lives in a ZIO fiber-local via `OpenTelemetry.contextZIO`. Pair only with the matching `Tracing` layer
  * — `contextJVM` is the variant for code that runs under the OpenTelemetry java-agent.
  */
object AppTracing {

  // `OpenTelemetry.custom` runs the inner ZIO in a Scope tied to the layer's lifetime, so each `ZIO.fromAutoCloseable`
  // here registers `close()` with the app scope. `BatchSpanProcessor.close()` flushes pending spans on shutdown.
  private def otlpSdkLayer(endpoint: String, serviceName: String): TaskLayer[JOpenTelemetry] =
    OpenTelemetry.custom(
      for {
        exporter <- ZIO.fromAutoCloseable(
          ZIO.attempt(OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build())
        )
        processor <- ZIO.fromAutoCloseable(
          ZIO.attempt(BatchSpanProcessor.builder(exporter).build())
        )
        tracerProvider <- ZIO.fromAutoCloseable(
          ZIO.attempt(
            SdkTracerProvider
              .builder()
              .setResource(Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, serviceName)))
              .addSpanProcessor(processor)
              .build()
          )
        )
        sdk <- ZIO.fromAutoCloseable(
          ZIO.attempt(OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build())
        )
      } yield sdk
    )

  val live: ZLayer[OtelConfig, Throwable, Tracing] =
    ZLayer.service[OtelConfig].flatMap { env =>
      val cfg = env.get
      val sdkLayer: ZLayer[Any, Throwable, JOpenTelemetry] = cfg.tracing match {
        case OtelTracing.Enabled(endpoint) => otlpSdkLayer(endpoint, cfg.serviceName)
        case OtelTracing.Disabled          => OpenTelemetry.noop
      }
      sdkLayer >+> OpenTelemetry.contextZIO >>> OpenTelemetry.tracing(cfg.serviceName)
    }
}
