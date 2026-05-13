val sbtScalafmt             = "2.6.0"
val sbtScalafix             = "0.14.6"
val sbtTpolecat             = "0.5.3"
val sbtRewarn               = "0.1.3"
val kindProjector           = "0.13.4"
val sbtRevolver             = "0.10.0"
val sbtBuildInfo            = "0.13.1"
val sbtNativePackager       = "1.11.7"
val betterMonadicFor        = "0.3.1"
val sbtDependencyLock       = "1.5.1"
val sbtSbom                 = "0.5.0"
val sbtExplicitDependencies = "0.3.1"

addSbtPlugin("org.scalameta"         % "sbt-scalafmt"              % sbtScalafmt)
addSbtPlugin("ch.epfl.scala"         % "sbt-scalafix"              % sbtScalafix)
addSbtPlugin("io.spray"              % "sbt-revolver"              % sbtRevolver)
addSbtPlugin("org.typelevel"         % "sbt-tpolecat"              % sbtTpolecat)
addSbtPlugin("com.timushev.sbt"      % "sbt-rewarn"                % sbtRewarn)
addSbtPlugin("com.eed3si9n"          % "sbt-buildinfo"             % sbtBuildInfo)
addSbtPlugin("com.github.sbt"        % "sbt-native-packager"       % sbtNativePackager)
addSbtPlugin("software.purpledragon" % "sbt-dependency-lock"       % sbtDependencyLock)
addSbtPlugin("com.github.sbt"        % "sbt-sbom"                  % sbtSbom)
addSbtPlugin("com.github.cb372"      % "sbt-explicit-dependencies" % sbtExplicitDependencies)
addCompilerPlugin("org.typelevel"    % "kind-projector"            % kindProjector cross CrossVersion.full)
addCompilerPlugin("com.olegpy"      %% "better-monadic-for"        % betterMonadicFor)

// Note: Optional plugins:
// (Left commented out to avoid unnecessary dependencies and/or conflicts)

// addDependencyTreePlugin
// sbt dependencyTree

// addSbtPlugin("org.jmotor.sbt" % "sbt-dependency-updates" % "1.2.7")
// sbt dependencyUpdates
