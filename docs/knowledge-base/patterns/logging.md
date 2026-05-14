# Logging

Structured logs through `zio-logging`. One console logger per JVM, format selected per-(app, env): pretty in local, JSON in deployed envs. Slf4j is bridged so library output (Quill, HikariCP, jul) passes through the same pipeline.

## Format and level

`LoggingConfig` (in `lib/common/.../impl/config/`) is a typed slice loaded from the `logging` block of the active `application-<env>.conf`:

```hocon
logging {
  format = "pretty"   # or "json"
  level  = "debug"    # trace|debug|info|warn|error|fatal
}
```

`format` switches between `consoleLogger` and `consoleJsonLogger`. `level` is the global minimum. Per `config-shape`, both fields are required — no defaults in code, no fallback if the block is missing.

## Wiring

`AppLogger.bootstrap` (in `lib/common/.../impl/logging/`) is the install layer. Every `ZIOAppDefault` entrypoint overrides its `bootstrap` field with it:

```scala
object ServerApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[Any, Any, Any] = AppLogger.bootstrap
  // ...
}
```

`ZIOAppDefault` builds the bootstrap layer before any layer in the main `run` effect, so the configured logger replaces ZIO's defaults before the first user-level log line emits — including `ConfigBootstrap.layer`'s `APP_ENV resolved: ...` line that fires once the main layer chain starts.

`bootstrap` does its own minimal config load: it reads `APP_ENV` directly, then calls `ConfigBootstrap.load[LoggingConfig]("logging")` against the parsed `EnvLabel`. This duplicates what `ConfigBootstrap.layer` + `LoggingConfig.load` would do later — the price of installing the logger before any layer in the main env builds. Typesafe-config caches the parsed file, so the duplicate read is free.

`LoggingConfig` exists as a typed slice and a reader, but has no `layer` — `AppLogger.bootstrap` is the only consumer. `ConfigBootstrap.readEnvLabel` is the silent variant of `ConfigBootstrap.layer` (no `APP_ENV resolved` log) used by the bootstrap.

## INFO vs DEBUG

The split is **operator vs investigator**:

- **INFO** — notable events an operator wants to see in production logs. This is the high-signal layer — what you scan first when chasing a bug. Three kinds of INFO sites:
  - *Per app-service call.* Every public app-service method logs once at completion, carrying outcome as annotations (ids, counts, branch taken). Long-running calls additionally log at start so in-flight work is visible. App-service is the natural per-request boundary — HTTP-driven and cross-context calls both pass through here, so a single line covers both surfaces.
  - *Notable events at lower layers.* When a domain service or repo does something an operator would want to know about — retry succeeded after N attempts, fallback path taken, cache invalidated, stale row repaired — it logs at INFO too.
  - *Process lifecycle and dev actions.* Boot, shutdown, config resolution, seed/migration milestones (`ServerApp`, `ConfigBootstrap`, `SeedExample`).
- **DEBUG** — per-iteration / per-call operational chatter. "Querying X", "skipping because Y", "already exists, no-op", per-row branch decisions, intermediate values. Lives at the layer where the interesting thing happens — mid-orchestration in an app service (e.g. `NotificationAppServiceImpl.list` annotates the dedup'd recipient count before the cross-context batch fetch), at a branch in a domain service (e.g. `CustomerServiceImpl.getMany` logs the empty-id-set short-circuit), inside a repo. Off in deployed envs by default; the investigator turns it on.
- **WARN** — recoverable failures that didn't surface to a user but might next time.
- **ERROR** — failures that surfaced or broke behavior. See "Error logs at boundaries" below for where they fire.

Logs are the primary observability surface — the dashboard (Grafana, GCP Log Viewer) is where you look first, traces are supplementary. So a log line should tell its own story without needing the trace alongside.

## Error logs at boundaries

Don't fail silently. Every boundary where a typed error could be lost or converted gets an ERROR log. Duplicate logs across boundaries are fine; missing one is not.

Concretely:

- **HTTP route handlers** — log before `mapError(renderAppFailure)` so the log keeps the typed `category`, `reason`, and HTTP status as queryable annotations; the response gets the converted `ErrorTO`.
- **Catchall handlers, RPC handlers, message handlers** — anywhere `mapError` / `catchAll` / `tapError` converts a typed failure to a different shape.
- **Forked fibers** (`forkDaemon`, `forkScoped`) — the forked effect must catch and log its own failures; the parent's `tapError` doesn't see them.
- **App's top-level entrypoint** — `tapErrorCause` at the outermost effect, last-resort log before the runtime swallows the cause.

Funnel typed-error logging through one helper: `LogError.tagged(context)(failure: AppFailure)` (in `lib/common/.../impl/logging/`). It emits `ZIO.logErrorCause` (full stack preserved) with `error.category`, `error.reason`, `error.status_code`, and `error.message` annotations from the AppFailure shape. The headline carries `failure.description` (the wire-safe summary; `BackendError` subclasses collapse this to `"Internal server error"`). `error.message` carries `failure.getMessage` so the diagnostic detail stays queryable as a flat structured field.

```scala
api.get(CustomerId(id))
  .map(c => Response.json(c.toJson))
  .tapError(LogError.tagged("CustomerRoutes.get"))   // logs typed shape; passes failure through
  .mapError(renderAppFailure)                         // converts to wire response
```

Top-level for the server:

```scala
serve.tapErrorCause(cause => ZIO.logErrorCause("Server crashed", cause)).provide(ServerEnv.layer)
```

## Test runs

`TestLogger.layer` (in `lib/common/src/test/scala/com/example/common/test/`, alongside the other test fixtures) is silent by default. Override by setting `TEST_LOG_LEVEL=trace|debug|info|warn|error|fatal` to install a pretty console logger at that level. The justfile recipe takes the level as an arg:

```sh
just test-it          # silent
just test-it debug    # full DEBUG+ output (Hikari, Quill, service logs)
```

Per-spec invocations work the same way — `TEST_LOG_LEVEL` is read from the environment, and sbt's forked test JVM inherits it:

```sh
TEST_LOG_LEVEL=debug sbt "it/testOnly *TransactorSpec"
```

Integration specs extend `IntegrationSpec` (next to `TestLogger`), which overrides the spec's `bootstrap` layer with `TestLogger.layer >+> testEnvironment`. The bootstrap layer is where ZIO's runtime config — including the default-logger set — is mutable. A regular `ZLayer` composed via `provideShared` doesn't reach into the runtime, so default loggers leak through and emit log lines from inside the code under test even if you tried to remove them there.

zio-test failure reports print regardless of logger configuration, so a silent logger doesn't hide test outcomes.

## Slf4j bridge

`Slf4jBridge.initialize` is part of `AppLogger.bootstrap`. It routes `org.slf4j` calls into zio-logging — Quill's SQL logging, HikariCP's pool messages, anything aimed at slf4j flows through the configured format.

The build excludes legacy `commons-logging` and `log4j` 1.x in favor of `jcl-over-slf4j` and `log4j-over-slf4j` shims. See `loggingDep` and `logExcludeDep` in `project/Dependencies.scala`.

## Adding context to a log line

Use `ZIO.logAnnotate` for structured context, not string interpolation. The annotation renders as a key in JSON and `key=value` after the message in pretty:

```scala
// preferred — single-key form uses the (String, String) overload
ZIO.logAnnotate("notification_id", id.toString) {
  ZIO.logInfo("notification created")
}

// multi-key form takes a Set[LogAnnotation]
ZIO.logAnnotate(
  Set(
    LogAnnotation(key = "notification_id", value = id.toString),
    LogAnnotation(key = "recipient_id", value = recipientId.toString)
  )
) {
  ZIO.logInfo("notification created")
}

// avoid — context is opaque to log aggregators
ZIO.logInfo(s"notification created: id=$id recipient=$recipientId")
```

Annotations propagate through fiber-locals to all log calls inside the scope, including ones inside libraries that use slf4j (provided the bridge is installed).

## Request-scoped annotations and the access log

`RequestLogging` (in `lib/common/impl/http/server/middleware/`) holds the two HTTP-level middlewares. Applied at the composition root in `ServerApp`:

```scala
Server.serve(routes @@ accessLog @@ requestId)
```

- `requestId` annotates a fresh `request_id` UUID for the duration of each handler. Fiber-local — any `ZIO.log*` inside the handler or anything it calls carries the same id.
- `accessLog` is `Middleware.requestLogging` from zio-http. One INFO line per request at completion, carrying `method`, `url`, `status_code`, `duration_ms`, `response_size`, `request_size`. Covers success and failure paths.

zio-http's `@@` is left-associative and stacks via `transform`, so the rightmost operand is the outermost wrapper. Putting `requestId` rightmost keeps its annotation scope open while `accessLog` emits — the access line gets `request_id`, which is what you join handler-internal logs to.

Adding handler-internal logs is just `ZIO.logInfo(...)` — they pick up `request_id` automatically via the fiber-local. Stack additional annotations with `ZIO.logAnnotate` for handler-specific context (`customer_id`, etc.).
