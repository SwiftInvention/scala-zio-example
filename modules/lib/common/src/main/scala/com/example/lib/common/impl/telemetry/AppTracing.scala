package com.example.lib.common.impl.telemetry

import com.example.lib.common.impl.config.{OtelConfig, OtelTracing}
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
  *   - For `OtelTracing.Enabled`, HEAD-probes the configured endpoint via [[zio.http.Client]]; on success, builds a
  *     real `OpenTelemetrySdk` with an OTLP HTTP exporter, a `BatchSpanProcessor`, and `service.name` set from
  *     `serviceName`. On probe failure, falls back to `OpenTelemetry.noop` and logs a WARN — the server comes up either
  *     way. The probe exists to avoid `BatchSpanProcessor` spamming reconnect-failure logs against a collector that's
  *     known-down at boot.
  *   - For `OtelTracing.Disabled`, uses `OpenTelemetry.noop`. Span calls become cheap no-ops; consumers stay
  *     unconditional (no `if (tracingEnabled) tracing.span(...) else effect`). Boot emits an INFO line so the choice is
  *     visible in the log.
  *
  * Boot-time INFO/WARN matrix:
  *   - Enabled + probe success → INFO `"OTLP endpoint $endpoint reachable; tracing enabled"`
  *   - Enabled + probe failure → INFO per attempt, then WARN `"OTLP probe failed; falling back to noop tracing..."`
  *   - Disabled → INFO `"OTLP tracing disabled by config; using noop SDK"`
  *
  * All SDK builders are wrapped in `ZIO.fromAutoCloseable` so `BatchSpanProcessor.close()` flushes the in-flight batch
  * when the app's scope ends. Span context lives in a ZIO fiber-local via `OpenTelemetry.contextZIO`; `contextJVM` is
  * the variant for code that runs under the OpenTelemetry java-agent.
  */
object AppTracing {

  private val probeBudget: Duration    = 5.seconds
  private val probeRetryBase: Duration = 100.millis

  // `OpenTelemetry.custom` runs the inner ZIO in a Scope tied to the layer's lifetime, so each `ZIO.fromAutoCloseable`
  // here registers `close()` with the app scope. `BatchSpanProcessor.close()` flushes pending spans on shutdown.
  private def buildRealSdk(endpoint: URL, serviceName: String): ZIO[Scope, Throwable, JOpenTelemetry] =
    for {
      exporter <- ZIO.fromAutoCloseable(
        ZIO.attempt(OtlpHttpSpanExporter.builder().setEndpoint(endpoint.encode).build())
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

  /** HEAD-probe the configured OTLP endpoint. Any HTTP response — including 405 from a POST-only `/v1/traces` — proves
    * the host is reachable and speaking HTTP at the configured URL. Network errors (refused, DNS, unreachable) retry
    * inside `probeBudget`. Per-attempt errors log at INFO so an operator scanning the boot log sees retry activity;
    * whole-loop failure is logged WARN by the caller and routed to a noop SDK.
    */
  private def probeReachable(endpoint: URL, client: Client): Task[Unit] = {
    val attempt = client
      .batched(Request.head(endpoint))
      .tapError(e => ZIO.logInfo(s"OTLP probe attempt failed for ${endpoint.encode}: ${e.getMessage}"))

    attempt
      .retry(Schedule.exponential(probeRetryBase))
      .timeoutFail(
        new RuntimeException(s"OTLP endpoint ${endpoint.encode} not reachable within ${probeBudget.render}")
      )(probeBudget)
      .unit
  }

  /** Probe the collector, then either build the real OTLP SDK or fall back to noop. The server starts either way. */
  private def probedOrNoop(endpoint: URL, serviceName: String, client: Client): TaskLayer[JOpenTelemetry] =
    OpenTelemetry.custom(
      probeReachable(endpoint, client).foldZIO(
        failure =>
          ZIO
            .logWarning(
              s"OTLP probe failed; falling back to noop tracing for this lifetime: ${failure.getMessage}"
            )
            .as(JOpenTelemetry.noop()),
        _ =>
          ZIO.logInfo(s"OTLP endpoint ${endpoint.encode} reachable; tracing enabled") *>
            buildRealSdk(endpoint, serviceName)
      )
    )

  private val disabledNoop: TaskLayer[JOpenTelemetry] =
    OpenTelemetry.custom(
      ZIO.logInfo("OTLP tracing disabled by config; using noop SDK").as(JOpenTelemetry.noop())
    )

  val live: ZLayer[OtelConfig & Client, Throwable, Tracing] =
    ZLayer.service[OtelConfig].flatMap { cfgEnv =>
      ZLayer.service[Client].flatMap { clientEnv =>
        val cfg    = cfgEnv.get
        val client = clientEnv.get
        val sdkLayer: TaskLayer[JOpenTelemetry] = cfg.tracing match {
          case OtelTracing.Enabled(endpoint) =>
            probedOrNoop(endpoint = endpoint, serviceName = cfg.serviceName, client = client)
          case OtelTracing.Disabled => disabledNoop
        }
        sdkLayer >+> OpenTelemetry.contextZIO >>> OpenTelemetry.tracing(cfg.serviceName)
      }
    }
}
