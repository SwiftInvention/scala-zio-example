import Dependencies._
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version      := "0.1.1-SNAPSHOT"

// quill-jdbc-zio pulls zio-json 0.7.3; zio-http pulls zio-schema-json which requires
// zio-json 0.9.0. Both 0.x — early-semver flags the gap. Letting the newer win.
ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % "always"

Global / onChangedBuildSource := ReloadOnSourceChanges

// sbt-explicit-dependencies — see patterns/supply-chain-security.md.
// These filters suppress two known classes of false positive that the plugin
// can't see through:
//
//  - **Runtime-only deps** (`unusedCompileDependenciesFilter`): JDBC drivers,
//    slf4j bridges, the plain `quill-jdbc` shim — never imported in Scala
//    source but required on the runtime classpath. The plugin does compile-
//    time bytecode analysis only, so they read as "unused".
//
//  - **Umbrella sub-artifacts** (`undeclaredCompileDependenciesFilter`):
//    bytecode references to sub-artifacts pulled in by a declared umbrella
//    (e.g. `pureconfig-core` via the `pureconfig` umbrella, `quill-core` /
//    `quill-engine` via `quill-jdbc-zio`, ZIO type-tag / fiber-instrumentation
//    internals via the `zio` umbrella).
val explicitDepsFilters = Seq(
  // Runtime-only.
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "com.mysql", name = "mysql-connector-j"),
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "org.slf4j", name = "jcl-over-slf4j"),
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "org.slf4j", name = "jul-to-slf4j"),
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "org.slf4j", name = "log4j-over-slf4j"),
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "dev.zio", name = "zio-logging-slf4j-bridge"),
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "io.getquill", name = "quill-jdbc"),
  // Declared as the public umbrella; imports route through sub-artifacts.
  unusedCompileDependenciesFilter -=
    moduleFilter(organization = "com.github.pureconfig", name = "pureconfig"),
  // Umbrella sub-artifacts — bytecode refs, no direct declarations.
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "com.github.pureconfig", name = "pureconfig-core"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "com.github.pureconfig", name = "pureconfig-generic"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "com.github.pureconfig", name = "pureconfig-generic-base"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "com.chuusai", name = "shapeless"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "com.typesafe", name = "config"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.getquill", name = "quill-core"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.getquill", name = "quill-engine"),
  // quill-jdbc-zio macro-expanded queries reference quill-jdbc classes at the bytecode level even when
  // source imports only touch the ZIO API. Treated as a sub-artifact of the declared quill-jdbc-zio.
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.getquill", name = "quill-jdbc"),
  // ZIO internals — type-tag / fiber-tracer / macro helpers.
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "dev.zio", name = "izumi-reflect"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "dev.zio", name = "zio-stacktracer"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "dev.zio", name = "zio-schema-macros"),
  // OTel SDK sub-artifacts — pulled by `opentelemetry-sdk` declared in otelSdkDep.
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.opentelemetry", name = "opentelemetry-api"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.opentelemetry", name = "opentelemetry-sdk-common"),
  undeclaredCompileDependenciesFilter -=
    moduleFilter(organization = "io.opentelemetry", name = "opentelemetry-sdk-trace")
)

lazy val commonSettings = Seq(
  run / fork  := true,
  Test / fork := true, // pins test cwd to the module's baseDirectory — required for SnapshotSpec's relative paths
  tpolecatScalacOptions += ScalacOptions.other("-Ymacro-annotations"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  libraryDependencies ++= zioTestDep,
  // Disable scaladoc HTML generation across all modules — the template publishes no library
  // artifacts, so the generated HTML has no consumer. Source-level doc comments still exist
  // (humans + agents read those), only the doc *task* is skipped.
  Compile / doc / sources                := Seq.empty,
  Compile / packageDoc / publishArtifact := false
) ++ explicitDepsFilters

// ── lib ─────────────────────────────────────────────────────

// libCommon holds cross-cutting concerns that aren't database-specific: effects/errors/ids/config/telemetry, plus the
// HTTP infrastructure (typed-Endpoint wire format `ApiFailure`, operational probes `HealthEndpoints`/`HealthRoutes`,
// shared middleware `RequestLogging`/`RequestTracing`, and the outbound `AppHttpClient`). `-api` modules only import
// wire-contract pieces; nothing forces them to reach into server-side code, and transitively pulling `zio-http`'s
// classpath is an acceptable cost given the alternative (a second shared lib).
lazy val libCommon = (project in file("modules/lib/common"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioPreludeDep ++ zioSchemaDep ++ zioSchemaDerivationDep ++ zioSchemaJsonTestDep ++
        enumeratumDep ++ pureconfigDep ++ loggingDep ++ zioOtelDep ++ otelSdkDep ++ zioHttpDep,
    excludeDependencies ++= logExcludeDep
  )

// libDb owns the shared MySQL schema and the persistence infrastructure that sits on it: `Transactor` + impl,
// `SqlContext`, `DataSourceLayer`, all PEs and DbSchema traits, NewType encodings, and the Flyway migrations under
// `src/main/resources/db/migration/`. The DB is one schema for the whole deployment, and centralizing it here means
// cross-ctx reads of foreign PEs are an ordinary import rather than a special case. `DbProbe` (defined in libCommon)
// is implemented here as `SqlDbProbe`.
//
// dbRuntimeDep (driver + plain JDBC shim) lives here so the deployment unit has the MySQL driver on its classpath at
// runtime. Ctx repo modules only need `dbDep` (the Quill ZIO API).
lazy val libDb = (project in file("modules/lib/db"))
  .dependsOn(libCommon % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioPreludeDep ++ pureconfigDep ++ dbDep ++ dbRuntimeDep ++ hikariDep)

// ── ctx: customer ───────────────────────────────────────────

lazy val ctxCustomerApi = (project in file("modules/ctx/customer-api"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioPreludeDep ++ zioSchemaDep ++ zioSchemaDerivationDep)

lazy val ctxCustomer = (project in file("modules/ctx/customer"))
  .dependsOn(libCommon % "compile->compile;test->test", libDb % "compile->compile;test->test", ctxCustomerApi)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioHttpDep ++ zioPreludeDep ++ zioSchemaDep ++ zioSchemaDerivationDep ++ enumeratumDep ++ dbDep
  )

// ── ctx: notification ───────────────────────────────────────

// No `ctxNotificationApi` module: nothing currently calls into notification from another ctx, so the cross-context
// contract has no consumer to publish to. See `patterns/cross-context-call.md` — the `-api` module appears when a
// foreign ctx imports it.
//
// ctxNotification depends on ctxCustomerApi: notification's app service composes a customer existence check + recipient
// enrichment via `CustomerApi`. This is the only cross-context coupling — at the impl layer, against the api contract.
lazy val ctxNotification = (project in file("modules/ctx/notification"))
  .dependsOn(
    libCommon % "compile->compile;test->test",
    libDb     % "compile->compile;test->test",
    ctxCustomerApi
  )
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioHttpDep ++ zioPreludeDep ++ zioSchemaDep ++ zioSchemaDerivationDep ++ enumeratumDep ++ dbDep
  )

// ── app: server ─────────────────────────────────────────────

lazy val appServer = (project in file("modules/app/server"))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, DockerPlugin)
  .dependsOn(libCommon, libDb, ctxCustomerApi, ctxCustomer, ctxNotification)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(dockerSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioHttpDep ++ pureconfigDep ++ zioOtelDep,
    excludeDependencies ++= logExcludeDep,
    name := "server"
  )

// ── app: dev (local-only dev tools) ─────────────────────────
// Local-only by build: `publish / skip := true` keeps the artifact off any deployment.
// Carries one-off scripts (data seeding, scratch experiments). Today's actions only need libCommon + libDb, but the
// module depends on every ctx so a new action can reach for ctx layers without first touching build.sbt.

lazy val appDev = (project in file("modules/app/dev"))
  .dependsOn(libCommon, libDb, ctxCustomerApi, ctxCustomer, ctxNotification)
  .settings(commonSettings)
  .settings(
    publish / skip      := true,
    Compile / mainClass := Some("com.example.app.dev.Experiment"),
    libraryDependencies ++=
      zioCoreDep ++ zioPreludeDep ++ pureconfigDep ++ dbDep,
    excludeDependencies ++= logExcludeDep,
    name := "dev"
  )

// ── app: integration-tests ──────────────────────────────────
// Single integration-test project per the sbt 1.9.0+ recommendation.
// Assumes infra is up — `just db-up && just db-migrate` runs externally before the test session.

lazy val appIntegrationTests = (project in file("modules/app/integration-tests"))
  .dependsOn(
    appServer,
    libCommon       % "test->test",
    libDb           % "test->test",
    ctxCustomer     % "test->test",
    ctxNotification % "test->test"
  )
  .settings(commonSettings)
  .settings(
    publish / skip := true
    // No compile-scope deps — appIntegrationTests is test-only (test src dir). Test framework + transitively-needed
    // libraries come via commonSettings' zioTestDep and the `test->test` deps on appServer/libCommon/etc.
  )

// ── root aggregator ─────────────────────────────────────────

lazy val root = (project in file("."))
  .aggregate(
    libCommon,
    libDb,
    ctxCustomerApi,
    ctxCustomer,
    ctxNotification,
    appServer,
    appDev,
    appIntegrationTests
  )
  .settings(name := "scala-zio-example")

lazy val buildInfoSettings = Seq(
  buildInfoKeys    := Seq[BuildInfoKey](name, version),
  buildInfoPackage := organization.value
)

// Docker image settings for the `server` deployment unit. Produces
// `scala-zio-example-server:<version>` and `:latest` via
// `sbt appServer/Docker/publishLocal` (see `patterns/docker-build.md`).
lazy val dockerSettings = Seq(
  dockerBaseImage        := "eclipse-temurin:21-jre-noble",
  Docker / packageName   := "scala-zio-example-server",
  Docker / version       := version.value,
  dockerUpdateLatest     := true,
  dockerExposedPorts     := Seq(8080),
  dockerRepository       := None,
  Docker / daemonUserUid := Some("1001"),
  Docker / daemonUser    := "app",
  Docker / daemonGroup   := "app",
  dockerLabels := Map(
    "org.opencontainers.image.title"       -> "scala-zio-example-server",
    "org.opencontainers.image.description" -> "Reference Scala+ZIO server",
    "org.opencontainers.image.version"     -> version.value,
    "org.opencontainers.image.source"      -> "https://github.com/VKFisher/scala-zio-example"
  )
)
