import Dependencies._
import org.typelevel.scalacoptions.ScalacOptions

ThisBuild / organization := "com.example"
ThisBuild / scalaVersion := "2.13.18"
ThisBuild / version      := "0.1.1-SNAPSHOT"

// quill-jdbc-zio pulls zio-json 0.7.3; zio-http pulls zio-schema-json which requires
// zio-json 0.9.0. Both 0.x — early-semver flags the gap. Letting the newer win.
ThisBuild / libraryDependencySchemes += "dev.zio" %% "zio-json" % "always"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val commonSettings = Seq(
  run / fork  := true,
  Test / fork := true, // pins test cwd to the module's baseDirectory — required for SnapshotSpec's relative paths
  tpolecatScalacOptions += ScalacOptions.other("-Ymacro-annotations"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  libraryDependencies ++= zioTestDep
)

// ── lib ─────────────────────────────────────────────────────

lazy val libCommon = (project in file("modules/lib/common"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioPreludeDep ++ zioSchemaDep ++ enumeratumDep ++ dbDep ++ pureconfigDep ++ loggingDep ++ telemetryDep,
    excludeDependencies ++= logExcludeDep
  )

// HTTP-flavored cross-cutting infrastructure: ApiFailure (typed-Endpoint wire format), shared probes (HealthRoutes),
// shared middleware (RequestLogging, RequestTracing), and the ServerRoutes composer used by every deployment unit.
// Sits above libCommon so the cross-context contract (`ctxCustomerApi`) doesn't transitively pull a server framework.
lazy val libHttp = (project in file("modules/lib/http"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioHttpDep ++ telemetryDep)

// ── ctx: customer ───────────────────────────────────────────

lazy val ctxCustomerApi = (project in file("modules/ctx/customer-api"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep)

lazy val ctxCustomer = (project in file("modules/ctx/customer"))
  .dependsOn(libCommon % "compile->compile;test->test", libHttp, ctxCustomerApi)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioHttpDep ++ dbDep)

// ── ctx: notification ───────────────────────────────────────

// notification-api intentionally does NOT depend on customer-api. notification's wire format references customers by
// id only; its own `NotificationRecipientTO` is a self-contained projection (id + display name + email). This keeps the
// two contracts independent — customer-api can evolve `CustomerTO` without rippling into notification's wire shape.
// See `patterns/cross-context-call.md`.
lazy val ctxNotificationApi = (project in file("modules/ctx/notification-api"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep)

// ctxNotification depends on ctxCustomerApi: notification's app service composes a customer existence check + recipient
// enrichment via `CustomerApi`. This is the only cross-context coupling — at the impl layer, against the api contract.
lazy val ctxNotification = (project in file("modules/ctx/notification"))
  .dependsOn(libCommon % "compile->compile;test->test", libHttp, ctxNotificationApi, ctxCustomerApi)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioHttpDep ++ dbDep)

// ── app: server ─────────────────────────────────────────────

lazy val appServer = (project in file("modules/app/server"))
  .enablePlugins(BuildInfoPlugin, JavaAppPackaging, DockerPlugin)
  .dependsOn(libCommon, libHttp, ctxCustomerApi, ctxCustomer, ctxNotificationApi, ctxNotification)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(dockerSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioHttpDep ++ pureconfigDep ++ loggingDep ++ dbDep,
    excludeDependencies ++= logExcludeDep,
    name := "server"
  )

// ── app: dev (local-only dev tools) ─────────────────────────
// Local-only by build: `publish / skip := true` keeps the artifact off any deployment.
// Carries one-off scripts (data seeding, scratch experiments) that share the production
// layer stack but never need to run in a deployed environment.

lazy val appDev = (project in file("modules/app/dev"))
  .dependsOn(libCommon, ctxCustomerApi, ctxCustomer, ctxNotificationApi, ctxNotification)
  .settings(commonSettings)
  .settings(
    publish / skip      := true,
    Compile / mainClass := Some("com.example.app.dev.Experiment"),
    libraryDependencies ++= zioCoreDep ++ pureconfigDep ++ loggingDep ++ dbDep,
    excludeDependencies ++= logExcludeDep,
    name := "dev"
  )

// ── app: it (integration tests) ─────────────────────────────
// Single integration-test project per the sbt 1.9.0+ recommendation
// Assumes infra is up — `just db-up && just db-migrate` runs externally before the test session.

lazy val it = (project in file("modules/app/it"))
  .dependsOn(
    appServer,
    libCommon % "test->test",
    libHttp,
    ctxCustomer     % "test->test",
    ctxNotification % "test->test"
  )
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= zioCoreDep ++ dbDep
  )

// ── root aggregator ─────────────────────────────────────────

lazy val root = (project in file("."))
  .aggregate(
    libCommon,
    libHttp,
    ctxCustomerApi,
    ctxCustomer,
    ctxNotificationApi,
    ctxNotification,
    appServer,
    appDev,
    it
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
