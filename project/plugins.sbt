val sbtScalafmt = "2.4.6"
val sbtScalafix = "0.10.4"
val kindProjector = "0.13.2"
val betterMonadicFor = "0.3.1"

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % sbtScalafmt)
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % sbtScalafix)
addCompilerPlugin("org.typelevel" % "kind-projector" % kindProjector cross CrossVersion.full)
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicFor)

// Note: Optional plugins:
// (Left commented out to avoid unnecessary dependencies and/or conflicts)

// addDependencyTreePlugin
// sbt dependencyTree

// addSbtPlugin("org.jmotor.sbt" % "sbt-dependency-updates" % "1.2.7")
// sbt dependencyUpdates

// addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.2.16")
// sbt undeclaredCompileDependencies
// sbt unusedCompileDependencies