# Tracing

Distributed tracing through `zio-telemetry` on top of the OpenTelemetry Java SDK. Spans are emitted via OTLP HTTP to a configured collector. Local dev uses Jaeger all-in-one (UI at `http://localhost:16686`). Tracing turns into cheap no-ops when disabled — call sites stay unconditional.

## Config

`OtelConfig` (in `lib/common/.../impl/config/`) is a typed slice loaded from the `otel` block:

```hocon
otel {
  service-name  = "scala-zio-example"
  otlp-endpoint = "http://localhost:4318/v1/traces"   # or null/absent to disable tracing
}
```

- `service-name` — the `service.name` resource attribute on every span. Required.
- `otlp-endpoint` — full OTLP HTTP traces URL. The HOCON value is a nullable string; the case-class field `tracing` is the ADT `OtelTracing.Enabled(url) | OtelTracing.Disabled`. A present URL parses to `Enabled`; absent or `null` parses to `Disabled`. The ADT makes the binary "tracing on / off" semantic explicit at the consumer site.

Per `config-shape`, `Disabled` is "tracing genuinely off", not a placeholder for a baked-in default. For `dev` and `prod` the value is `${OTEL_ENDPOINT}` (required at deploy); deployments that don't run a collector remove the line from their conf.

## Wiring

`AppTracing.live` (in `lib/common/.../impl/telemetry/`) consumes `OtelConfig` plus `zio.http.Client` and produces `zio.telemetry.opentelemetry.tracing.Tracing`:

```text
OtelConfig.layer  → OtelConfig
AppHttpClient.layer → Client
AppTracing.live   → Tracing      (OTLP exporter when Enabled; OpenTelemetry.noop when Disabled)
```

The SDK builders are wrapped in `ZIO.fromAutoCloseable`, so `BatchSpanProcessor.close()` flushes the in-flight batch when the app's scope ends.

Before the SDK is built, `AppTracing.live` HEAD-probes the configured OTLP endpoint via `Client` with bounded exponential-backoff retry inside a fixed budget. An unreachable collector otherwise emits a stream of `BatchSpanProcessor` reconnect failures into the log; the probe makes the layer fail at boot instead. Any HTTP response counts — including `405 Method Not Allowed` or `404 Not Found` — the probe asserts "the host is reachable and speaking HTTP at this URL", not "the OTLP path and method are correct".

Boot-time and runtime are asymmetric on purpose: an unreachable collector at boot fails the layer; an unreachable collector during runtime doesn't — `BatchSpanProcessor` logs export failures via the OpenTelemetry SDK and keeps the server serving traffic.

Span context propagation uses `OpenTelemetry.contextZIO`, which stores the active span in a ZIO fiber-local. The alternative `contextJVM` exists for code running under the OpenTelemetry java-agent — pick the variant that matches your deployment, and use the same one consistently.

## HTTP server spans

`RequestTracing.span` (in `lib/common/http/server/middleware/`) is `Middleware[Tracing]` that opens one span per HTTP request. Applied at `ServerApp` alongside the logging middlewares:

```scala
Server.serve(routes @@ RequestTracing.span @@ accessLog @@ requestId)
```

Each span is named `<METHOD> <path>` with kind `SERVER`. The handler runs inside the span scope; the span closes when the handler completes. Span status follows HTTP semantics via a custom `StatusMapper`: 5xx response → `ERROR`, anything else (including 4xx) → `OK`. zio-http's `Routes[R, Response]` puts both 4xx and 5xx in the failure channel, so without the mapper a 404 would tag the span as a span error — the mapper inverts that to "the server responded correctly to a bad request, the span is OK."

Inbound trace propagation (reading `traceparent` headers to chain under an upstream caller's trace) is intentionally not wired. Spans here are root spans. To pick up upstream context, swap `tracing.span` for `tracing.extractSpan(TraceContextPropagator.default, IncomingContextCarrier.default(headers), ...)`.

## No internal spans by default

Internal layers (app-service, repo, DirectClient) are **not** wrapped in spans. The reasons:

- zio-telemetry's `contextZIO` path has no automatic instrumentation; every method would have to be wrapped by hand. With `Tracing` injected as a constructor dep and `effect @@ tracing.aspects.span("name")` per public method, that's ~24 wrappings across our impls plus a `& Tracing` clause in every layer signature.
- The HTTP middleware span plus the access-log line cover most operator needs at the request level.
- Tighter trace fidelity is rarely the cheapest investigation tool — for "why is this slow?" you usually want the access-log duration and a metric, not a sub-span; for "what happened in this request?" you usually want the structured logs that the request_id annotation correlates.

When a specific investigation does need a sub-span, add it ad-hoc:

```scala
import tracing.aspects._
override def get(id: CustomerId): AppIO[Customer] =
  customerService.get(id) @@ span("CustomerService.get")
```

If a pattern emerges across many places, lift it back into a principle and apply consistently.

## Local Jaeger

`docker-compose.yml` includes a `jaeger` service (`jaegertracing/all-in-one:1.76.0`) alongside MySQL. Both come up via `just local-infra-up` (and back down via `just local-infra-down`). UI at `http://localhost:16686`, OTLP HTTP on `4318`.

The Jaeger container is persistence-less, so restarts wipe history. That's fine for dev; production deployments choose their own collector (Tempo, SigNoz, Honeycomb, a vendored OTLP gateway).
