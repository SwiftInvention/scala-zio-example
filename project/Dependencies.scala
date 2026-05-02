import sbt._
import sbt.librarymanagement.InclExclRule

object Dependencies {

  // ── ZIO core ────────────────────────────────────────────────
  lazy val zioCoreDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio
  )

  // ── HTTP ───────────────────────────────────────────────────
  lazy val zioHttpDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-http" % Versions.zioHttp
  )

  lazy val zioJsonDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-json" % Versions.zioJson
  )

  lazy val tapirDep: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"      % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-newtype"           % Versions.tapir
  )

  // ── Config ─────────────────────────────────────────────────
  lazy val pureconfigDep: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
  )

  // ── DB ─────────────────────────────────────────────────────
  lazy val dbDep: Seq[ModuleID] = Seq(
    "io.getquill" %% "quill-jdbc"        % Versions.quill,
    "io.getquill" %% "quill-jdbc-zio"    % Versions.quill,
    "com.mysql"    % "mysql-connector-j" % Versions.mysql,
    "org.flywaydb" % "flyway-core"       % Versions.flyway,
    "org.flywaydb" % "flyway-mysql"      % Versions.flyway
  )

  // ── Mapping / utility ──────────────────────────────────────
  lazy val chimneyDep: Seq[ModuleID] = Seq(
    "io.scalaland" %% "chimney" % Versions.chimney
  )

  lazy val newtypeDep: Seq[ModuleID] = Seq(
    "io.estatico" %% "newtype" % Versions.newType
  )

  lazy val zioPreludeDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-prelude" % Versions.zioPrelude
  )

  lazy val enumeratumDep: Seq[ModuleID] = Seq(
    "com.beachape" %% "enumeratum"       % Versions.enumeratum,
    "com.beachape" %% "enumeratum-quill" % Versions.enumeratum,
    "com.beachape" %% "enumeratum-circe" % Versions.enumeratum
  )

  lazy val circeDep: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic-extras" % Versions.circe
  )

  // ── Logging ────────────────────────────────────────────────
  lazy val loggingDep: Seq[ModuleID] = Seq(
    "dev.zio"  %% "zio-logging"              % Versions.zioLogging,
    "dev.zio"  %% "zio-logging-slf4j-bridge" % Versions.zioLogging,
    "org.slf4j" % "jcl-over-slf4j"           % Versions.slf4j,
    "org.slf4j" % "log4j-over-slf4j"         % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j"             % Versions.slf4j
  )

  lazy val logExcludeDep: Seq[InclExclRule] = Seq(
    ExclusionRule("commons-logging", "commons-logging"),
    ExclusionRule("log4j", "log4j")
  )
}
