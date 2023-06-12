import Dependencies._

ThisBuild / organization := "org.organization"
ThisBuild / scalaVersion := "2.13.11"
ThisBuild / version      := "0.1.1-SNAPSHOT"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scalaZioExample = (project in file("scala-zio-example"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    settings
      ++ integrationTestSettings
      ++ buildInfoSettings
      ++ Seq(name := """scala-zio-example""")
  )

lazy val scalaZioExampleRoot = project
  .in(file("."))
  .aggregate(scalaZioExample)

lazy val settings = Seq(
  libraryDependencies ++= commonDep ++ testDep ++ httpDep ++ dbDep ++ logDep,
  excludeDependencies ++= logExcludeDep,
  run / fork := true,
  tpolecatScalacOptions += ScalacOptions.other("-Ymacro-annotations"),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val integrationTestSettings =
  inConfig(IntegrationTest)(
    Seq(
      // To ensure that test containers get cleaned up immediately
      Test / fork := true
    )
  )

lazy val buildInfoSettings = Seq(
  buildInfoKeys    := Seq[BuildInfoKey](name, version),
  buildInfoPackage := organization.value
)
