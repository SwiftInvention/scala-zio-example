# HTTP Client

Outbound HTTP via `zio.http.Client`. One client is wired at the composition root and shared by every consumer that needs to call an external endpoint.

## Wiring

`AppHttpClient.layer` (in `lib/common/http/client/`) consumes typed `HttpClientConfig` and produces `zio.http.Client`:

```text
HttpClientConfig.layer → HttpClientConfig
AppHttpClient.layer    → Client            (configured with our connection-timeout and idle-timeout)
```

Under the hood it composes zio-http's `Client.live` with default `NettyConfig` and `DnsResolver` — if a deployment ever needs to tune those (custom resolver, fixed local address, SSL config beyond defaults), swap the providers at `AppHttpClient`.

## Config

```hocon
http-client {
  connection-timeout = 5s
  idle-timeout       = 30s
}
```

`connection-timeout` bounds per-request TCP connect attempts; `idle-timeout` is how long an idle pooled connection survives before close. Both are required — there are no defaults in code (`config-shape`).

Per-endpoint timeouts (full response read) belong on the caller via `effect.timeoutFail(...)(...)`.

Other zio-http knobs — connection-pool size, `requestDecompression`, SSL config beyond the JVM default — aren't exposed yet. Add fields per consumer demand: the field `requestDecompression` is the most likely first ask the moment a real upstream returns gzipped JSON.

## Consumer pattern

A consumer takes `Client` as a layer dependency and uses it like any other ZIO service:

```scala
final class ExternalApiClient(client: Client) extends ExternalApi {
  override def fetch(id: ExternalId): AppIO[ExternalRecord] =
    client
      .batched(Request.get(URL.decode(s"https://upstream/records/$id").toOption.get))
      .flatMap(_.body.asString)
      .flatMap(parse)
      .mapError(toAppFailure)
}
object ExternalApiClient {
  val layer: URLayer[Client, ExternalApi] = ZLayer.fromFunction(new ExternalApiClient(_))
}
```

Use `client.batched(request)` (auto-discharges the response body's `Scope`) unless you need streaming, in which case the response leaks `Scope` into the effect type and the caller must `ZIO.scoped` around it.

## Failures at the boundary

zio-http's `Client` fails with `Throwable` — network errors, parse errors, codec errors all surface there. Adapters that expose typed `AppFailure` to the rest of the app (`AppIO`) must `mapError` at the boundary, the same way `Transactor` and `SqlContext` lift JDBC `Throwable`s into `DbError`.

## First consumer: the OTLP startup probe

`AppTracing.live` HEAD-probes the configured OTLP endpoint at boot using this client. Any HTTP response counts as success; network/DNS errors retry inside a fixed budget. See [`tracing.md`](tracing.md) for what the probe does and doesn't catch.
