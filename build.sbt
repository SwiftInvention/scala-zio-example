import Dependencies._

ThisBuild / organization := "org.organization"
ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version      := "0.1.1-SNAPSHOT"

lazy val scalaZioExample = (project in file("scala-zio-example"))
  .settings(
    settings
      ++ Seq(
        name := """scala-zio-example"""
      )
  )

lazy val scalaZioExampleRoot = project
  .in(file("."))
  .aggregate(scalaZioExample)

lazy val settings = Seq(
  libraryDependencies ++= commonDep ++ testDep ++ httpDep ++ dbDep,
  scalacOptions ++= Seq(
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",         // Specify character encoding used by source files.
    "-explaintypes", // Explain type errors in more detail.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-language:higherKinds",         // Allow higher-kinded types
    "-language:postfixOps",          // Allow postfix operator notation, such as 1 to 10 toList
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked",       // Enable additional warnings where generated code depends on assumptions.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Ymacro-annotations", // Allow to use macro annotations which is needed in elastico/newtypes
    "-Ywarn-dead-code",    // Warn when dead code is identified.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",   // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",    // Warn if a local definition is unused.
    "-Ywarn-unused:params",    // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",   // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",  // Warn if a private member is unused.
    "-Ywarn-value-discard",    // Warn when non-Unit expression results are unused.
    "-Werror"                  // Fail the compilation if there are any warnings.
  ),

  // Scalafix
  semanticdbEnabled := true,                        // Enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // Only required for Scala 2.x
  scalafixOnCompile := true, // Run scalafix every time on compile. Comment out when necessary

  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)
