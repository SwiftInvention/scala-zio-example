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
    libraryDependencies ++= zioCoreDep ++ zioPreludeDep ++ zioJsonDep ++ enumeratumDep ++ dbDep ++ pureconfigDep
  )

// ── ctx: customer ───────────────────────────────────────────

lazy val ctxCustomerApi = (project in file("modules/ctx/customer-api"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioJsonDep)

lazy val ctxCustomer = (project in file("modules/ctx/customer"))
  .dependsOn(libCommon % "compile->compile;test->test", ctxCustomerApi)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioHttpDep ++ zioJsonDep ++ dbDep)

// ── app: server ─────────────────────────────────────────────

lazy val appServer = (project in file("modules/app/server"))
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(libCommon, ctxCustomerApi, ctxCustomer)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(
    libraryDependencies ++=
      zioCoreDep ++ zioHttpDep ++ zioJsonDep ++ pureconfigDep ++ loggingDep ++ dbDep,
    excludeDependencies ++= logExcludeDep,
    name := "server"
  )

// ── app: it (integration tests) ─────────────────────────────
// Single integration-test project per the sbt 1.9.0+ recommendation
// Assumes infra is up — `just db-up && just db-migrate` runs externally before the test session.

lazy val it = (project in file("modules/app/it"))
  .dependsOn(
    appServer,
    libCommon   % "test->test",
    ctxCustomer % "test->test"
  )
  .settings(commonSettings)
  .settings(
    publish / skip := true,
    libraryDependencies ++= zioCoreDep ++ dbDep
  )

// ── root aggregator ─────────────────────────────────────────

lazy val root = (project in file("."))
  .aggregate(libCommon, ctxCustomerApi, ctxCustomer, appServer, it)
  .settings(name := "scala-zio-example")

lazy val buildInfoSettings = Seq(
  buildInfoKeys    := Seq[BuildInfoKey](name, version),
  buildInfoPackage := organization.value
)
