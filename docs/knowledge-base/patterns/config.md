# Config

PureConfig case classes loaded from per-(app, env) HOCON files at boot. Fail-fast on missing or malformed config.

## Files

```text
modules/<app>/src/main/resources/
├── application-local.conf.example   committed; canonical local-dev shape with throwaway values
├── application-local.conf           gitignored; copied from .example by `just initial-setup`
├── application-dev.conf             committed; ${VAR} substitutions for secrets
└── application-prod.conf            committed; ${VAR} substitutions for secrets
```

Only `application-local.conf` is gitignored. `dev`/`prod` files are committed because they contain no real secrets — only defaults and `${VAR}` references. The `.example` template exists for `local` because local config typically has actual values baked in (passwords, API keys for dev convenience) that shouldn't be in git.

Each file is its own truth — no `reference.conf`, no merge with another env's file. The bug this guards against: a value changes in `reference.conf`, an env file doesn't mention it, the new default silently activates. With self-contained per-env files, "what's the runtime value?" is one lookup, and a new field has to be set in every env file (or boot fails there).

## Selecting the env at runtime

`APP_ENV=local|dev|prod` — required, no default. `ConfigBootstrap` reads it, parses to `EnvLabel`, and loads `application-${label.entryName}.conf` from the classpath. The app refuses to start if `APP_ENV` is unset, isn't a valid `EnvLabel`, or the resource is missing/empty.

## EnvLabel — the only thing that propagates from boot

`EnvLabel` (in `lib/common/domain/model/`) is a sealed enumeratum trait with `Local | Dev | Prod`. `entryName` is lowercase — matches both the conf-file suffix and the shell convention for env-var values.

`ConfigBootstrap.layer` (in `lib/common/impl/config/`) reads `APP_ENV`, parses to `EnvLabel`, exposes it. **Nothing else propagates.** No `Config` service, no `EnvConfig` wrapper. Typesafe-config and pureconfig stay contained to the parsing perimeter — the consumers downstream see only typed `XConfig` values.

`ConfigBootstrap` also exposes a helper used by every `XConfig.layer`:

```scala
def load[T: ConfigReader](path: String): ZIO[EnvLabel, AppFailure, T]
```

Each `XConfig` parses its own slice via this helper. The file is loaded inside the helper; HOCON's classloader caches it, so calling it from N different layers at boot is cheap.

`EnvLabel` and `ConfigBootstrap` live in `lib/common` so any additional deployment unit (worker, sync-job) reuses the same bootstrap.

## XConfigs are organized by purpose, not by ctx

There's no "ctx config" abstraction. Each `XConfig` lives where its purpose lives:

- Infra resources owned by lib → `lib/common/.../impl/<resource>/...Config.scala`
  - e.g. `DataSourceConfig` next to `DataSourceLayer`
- App-shaped concerns → `app/<name>/.../config/...Config.scala`
  - e.g. `ServerConfig` (HTTP host/port)
- Service-specific tunables in a ctx → `ctx/<name>/.../<...>/...Config.scala`
  - Granularity is per-service, not "the ctx config". A ctx may have zero, one, or many `XConfig`s.

The unification happens at the layer stack — every `XConfig.layer` is wired into composition. There is no central `AppConfig` aggregating them. Adding a new config doesn't touch a shared file.

## Defining an XConfig

```scala
final case class DataSourceConfig(jdbcUrl: String, user: String, password: String, maximumPoolSize: Int)

object DataSourceConfig {
  implicit val reader: ConfigReader[DataSourceConfig] = deriveReader[DataSourceConfig]

  val layer: ZLayer[EnvLabel, AppFailure, DataSourceConfig] =
    ZLayer.fromZIO(ConfigBootstrap.load[DataSourceConfig]("database.data-source"))
}
```

`deriveReader` (semiauto) puts the pureconfig instance in the case class's companion, so callers don't need `import pureconfig.generic.auto._`. Each `XConfig` declares its own derivation explicitly.

## Layer chain

```text
ConfigBootstrap.layer        : ZLayer[Any, AppFailure, EnvLabel]       // parses APP_ENV
  ↓
DataSourceConfig.layer       : ZLayer[EnvLabel, _, DataSourceConfig]   // ConfigBootstrap.load(...)
ServerConfig.layer           : ZLayer[EnvLabel, _, ServerConfig]       // ConfigBootstrap.load(...)
  ↓
DataSourceLayer.layer        : ZLayer[DataSourceConfig, _, DataSource]
(http server binding)        : ZLayer[ServerConfig, _, Server]
```

The parsing tier (XConfig.layers) is the only place that touches typesafe-config or pureconfig. Service layers below it consume typed values exclusively.

## No defaults in code — for configurable values

If a value is something an operator is meant to tune per env, it lives in `.conf` with no in-code fallback. The rule applies to all code that consumes a config field:

```scala
// banned — case-class default
final case class ServerConfig(host: String, port: Int = 8080)

// banned — getOrElse fallback in the consumer
val port = cfg.port.getOrElse(8080)

// banned — if-empty-then-default branch
val port = if (cfg.port.isEmpty) 8080 else cfg.port.get

// banned — pattern-match producing a fallback value
val port = cfg.port match { case Some(p) => p; case None => 8080 }
```

The question "what's `port` at runtime?" must always be answered by reading the active conf file — never by reading code. Code defaults split that question across two sources and let missing config silently activate a fallback.

### Internal-mechanism constants are not config

A `private val probeBudget: Duration = 5.seconds` in a layer's source is fine. It isn't a configurable value — operators don't tune the retry cadence on a startup probe; they turn the probe on or off (which is a config decision, modeled in `OtelConfig`). Promoting every internal constant to `.conf` would inflate the surface without adding control.

The test: is there a plausible env where an operator would want this value different? If yes, it's config. If it's mechanism the implementation chose for itself, it's a constant in code.

### Required vs optional fields

If a value is required: required field on the case class, no default. The `.conf` file holds the value (per env). PureConfig fails-fast if it's missing.

```scala
final case class DataSourceConfig(jdbcUrl: String, user: String, password: String, maximumPoolSize: Int)
```

If a value is *conceptually* optional — its absence has distinct semantics from any present value — model it as `Option[X]` and have the consumer branch on the option. **Not** to substitute a fallback value, but to do something genuinely different:

```scala
final case class TelemetryConfig(metricsSink: Option[MetricsSinkConfig])

// ok — None means "metrics off", not "use the default sink"
def init(cfg: TelemetryConfig): UIO[Unit] = cfg.metricsSink match {
  case Some(sink) => MetricsSink.start(sink)
  case None       => ZIO.unit
}
```

If `None` would mean "use this baked-in value", the field is required. Put the value in the `.conf`.

### Env-var substitution

Two forms, two uses:

```hocon
# ${VAR} — required, no fallback. Used in dev/prod files where the value
# *must* come from the deployment env. Boot fails if VAR is unset.
jdbc-url = ${MYSQL_URL}

# ${?VAR} — opt-in override, paired with an explicit default in the same file.
# Used in local.conf where a hardcoded value is the right default, but a
# deployment-shaped context (e.g. the devcontainer) wants to override it.
jdbc-url = "jdbc:mysql://localhost:3306/localDatabase?useUnicode=true&serverTimezone=UTC"
jdbc-url = ${?MYSQL_URL}
```

Bare `${?VAR}` without a same-file default is banned — that's the silent-default mode the rule prevents.

Every value has exactly one default in exactly one place (the active `.conf`). External env vars override but carry no defaults of their own.

## HOCON ↔ case-class field naming

PureConfig defaults to converting Scala `camelCase` fields to HOCON `kebab-case` keys. `jdbcUrl` ↔ `jdbc-url`, `maximumPoolSize` ↔ `maximum-pool-size`. HOCON convention is kebab-case; we follow it.

## Secrets

For `local`: real but throwaway values in the gitignored `application-local.conf`.

For `dev`/`prod`: `${VAR}` substitution, value sourced from the deployment env (k8s secret, vault, CI variable). The `.conf` file is committed; the value is not.
