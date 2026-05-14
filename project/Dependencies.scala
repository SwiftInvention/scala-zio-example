import sbt._
import sbt.librarymanagement.InclExclRule

object Dependencies {

  // ── ZIO core ────────────────────────────────────────────────
  lazy val zioCoreDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio
  )

  // ── ZIO test ────────────────────────────────────────────────
  // Test-scope only. Goes in `commonSettings` so any module can have specs.
  lazy val zioTestDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-test"     % Versions.zioTest % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.zioTest % Test
  )

  // ── HTTP ───────────────────────────────────────────────────
  lazy val zioHttpDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-http" % Versions.zioHttp
  )

  lazy val zioJsonDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-json" % Versions.zioJson
  )

  // zio-schema gives us `Schema[A]` for typed boundary serialization (zio-http Endpoint API, OpenAPI generation).
  // Pulled explicitly so modules that need Schema (lib/common, ctx/*-api) don't have to drag the full server
  // framework via `zioHttpDep`.
  lazy val zioSchemaDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-schema" % Versions.zioSchema
  )

  // zio-schema-derivation — `DeriveSchema.gen[A]` macro. Needed by modules that derive a Schema (api TOs,
  // libCommon's ErrorTO); pure-consumer modules only need `zioSchemaDep`.
  lazy val zioSchemaDerivationDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-schema-derivation" % Versions.zioSchema
  )

  // zio-schema-json — JsonCodec derived from a Schema. Only used in test specs (SnapshotSpec, TestServer).
  lazy val zioSchemaJsonTestDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-schema-json" % Versions.zioSchema % Test
  )

  // ── Config ─────────────────────────────────────────────────
  lazy val pureconfigDep: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
  )

  // ── DB ─────────────────────────────────────────────────────
  // Note: Flyway runs out-of-process via the `flyway` CLI (see justfile + .sdkmanrc).
  // No JVM-side Flyway dep — migrations are an explicit deployment step, not a boot-time effect.

  // The Quill ZIO API used by ctx repo impls (querySchema, run, etc.). Compile-time API only.
  lazy val dbDep: Seq[ModuleID] = Seq(
    "io.getquill" %% "quill-jdbc-zio" % Versions.quill
  )

  // Runtime plumbing: the plain Quill JDBC bindings (driver-loading shim) and the MySQL JDBC driver itself.
  // Only the deployment unit (libDb's DataSourceLayer + appServer/appDev) needs these on the classpath.
  lazy val dbRuntimeDep: Seq[ModuleID] = Seq(
    "io.getquill" %% "quill-jdbc"        % Versions.quill,
    "com.mysql"    % "mysql-connector-j" % Versions.mysql
  )

  // HikariCP — connection pool. Used directly in libDb's `DataSourceLayer`.
  lazy val hikariDep: Seq[ModuleID] = Seq(
    "com.zaxxer" % "HikariCP" % Versions.hikari
  )

  lazy val zioPreludeDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-prelude" % Versions.zioPrelude
  )

  lazy val enumeratumDep: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum" % Versions.enumeratum
  )

  // ── Logging ────────────────────────────────────────────────
  lazy val loggingDep: Seq[ModuleID] = Seq(
    "dev.zio"  %% "zio-logging"              % Versions.zioLogging,
    "dev.zio"  %% "zio-logging-slf4j-bridge" % Versions.zioLogging,
    "org.slf4j" % "jcl-over-slf4j"           % Versions.slf4j,
    "org.slf4j" % "log4j-over-slf4j"         % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j"             % Versions.slf4j
  )

  // ── Tracing ────────────────────────────────────────────────

  // zio-telemetry's OpenTelemetry binding — gives `Tracing.span`, `Tracing.aspects.*`, etc. Pulled by any module
  // that touches the `Tracing` service directly (appServer routes, libCommon middleware).
  lazy val zioOtelDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-opentelemetry" % Versions.zioTelemetry
  )

  // OTel Java SDK + OTLP HTTP exporter + semconv attribute keys. Used by `AppTracing` in libCommon to build the
  // tracer provider; downstream modules consume the layer and don't need to declare these directly.
  lazy val otelSdkDep: Seq[ModuleID] = Seq(
    "io.opentelemetry"         % "opentelemetry-sdk"           % Versions.openTelemetry,
    "io.opentelemetry"         % "opentelemetry-exporter-otlp" % Versions.openTelemetry,
    "io.opentelemetry.semconv" % "opentelemetry-semconv"       % Versions.openTelemetrySemconv
  )

  lazy val logExcludeDep: Seq[InclExclRule] = Seq(
    ExclusionRule("commons-logging", "commons-logging"),
    ExclusionRule("log4j", "log4j")
  )
}
