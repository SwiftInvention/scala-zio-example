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
import zio.http.{Client, Request, URL}
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
  * Two layer variants:
  *
  *   - [[live]] HEAD-probes the configured endpoint via [[zio.http.Client]] before building the SDK; if the collector
  *     isn't reachable inside the budget the layer fails at boot. Used by deployment composition roots.
  *   - [[liveWithoutProbe]] skips the probe. Used where no `Client` is wired (typically tests, where `OtelTracing` is
  *     `Disabled` and the SDK is `OpenTelemetry.noop` anyway).
  *
  * Span context lives in a ZIO fiber-local via `OpenTelemetry.contextZIO`. Pair only with the matching `Tracing` layer
  * — `contextJVM` is the variant for code that runs under the OpenTelemetry java-agent.
  */
object AppTracing {

  private val probeBudget: Duration    = 5.seconds
  private val probeRetryBase: Duration = 100.millis

  // `OpenTelemetry.custom` runs the inner ZIO in a Scope tied to the layer's lifetime, so each `ZIO.fromAutoCloseable`
  // here registers `close()` with the app scope. `BatchSpanProcessor.close()` flushes pending spans on shutdown.
  private def otlpSdkLayer(
      endpoint: String,
      serviceName: String,
      probe: Option[Client]
  ): TaskLayer[JOpenTelemetry] =
    OpenTelemetry.custom(
      for {
        _ <- probe match {
          case Some(client) => probeReachable(endpoint, client)
          case None         => ZIO.unit
        }
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

  /** HEAD-probe the configured OTLP endpoint. Any HTTP response — including 405 from a POST-only `/v1/traces` — proves
    * the host is reachable and speaking HTTP at the configured URL. Network errors (refused, DNS, unreachable) retry
    * inside `probeBudget`. Per-attempt errors are logged at DEBUG so the underlying cause is recoverable after a budget
    * timeout.
    */
  private def probeReachable(endpoint: String, client: Client): Task[Unit] =
    ZIO.fromEither(URL.decode(endpoint)).flatMap { url =>
      val attempt = client
        .batched(Request.head(url))
        .tapError(e => ZIO.logDebug(s"OTLP probe attempt failed for $endpoint: ${e.getMessage}"))

      attempt
        .retry(Schedule.exponential(probeRetryBase))
        .timeoutFail(
          new RuntimeException(s"OTLP endpoint $endpoint not reachable within ${probeBudget.render}")
        )(probeBudget)
        .tapError(e => ZIO.logError(s"OTLP startup probe failed: ${e.getMessage}"))
        .unit
    }

  private def buildLayer(probe: Option[Client]): ZLayer[OtelConfig, Throwable, Tracing] =
    ZLayer.service[OtelConfig].flatMap { env =>
      val cfg = env.get
      val sdkLayer: TaskLayer[JOpenTelemetry] = cfg.tracing match {
        case OtelTracing.Enabled(endpoint) =>
          otlpSdkLayer(endpoint = endpoint, serviceName = cfg.serviceName, probe = probe)
        case OtelTracing.Disabled => OpenTelemetry.noop
      }
      sdkLayer >+> OpenTelemetry.contextZIO >>> OpenTelemetry.tracing(cfg.serviceName)
    }

  val live: ZLayer[OtelConfig & Client, Throwable, Tracing] =
    ZLayer.service[Client].flatMap { clientEnv =>
      buildLayer(probe = Some(clientEnv.get))
    }

  val liveWithoutProbe: ZLayer[OtelConfig, Throwable, Tracing] =
    buildLayer(probe = None)
}
