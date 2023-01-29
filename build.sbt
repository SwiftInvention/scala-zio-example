import Dependencies._

ThisBuild / organization := "org.organization"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version      := "0.1.1-SNAPSHOT"

lazy val scalaZioExample = (project in file("scala-zio-example"))
  .settings(
    settings
      ++ integrationTestSettings
      ++ Seq(name := """scala-zio-example""")
  )

lazy val scalaZioExampleRoot = project
  .in(file("."))
  .aggregate(scalaZioExample)

lazy val settings = Seq(
  libraryDependencies ++= commonDep ++ testDep ++ httpDep ++ dbDep,
  tpolecatScalacOptions += ScalacOptions.other("-Ymacro-annotations"),
  // Scalafix
  semanticdbEnabled := true,                        // Enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // Only required for Scala 2.x
  scalafixOnCompile := true, // Run scalafix every time on compile. Comment out when necessary

  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

lazy val integrationTestSettings =
  inConfig(IntegrationTest)(
    Seq(
      // To ensure that test containers get cleaned up immediately
      Test / fork := true
    )
  )
