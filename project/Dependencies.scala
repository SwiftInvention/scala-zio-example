import sbt._
import sbt.librarymanagement.InclExclRule

object Dependencies {

  lazy val commonDep: Seq[ModuleID] = Seq(
    "dev.zio"                     %% "zio"           % Versions.zio,
    "com.github.pureconfig"       %% "pureconfig"    % Versions.pureConfig,
    "io.scalaland"                %% "chimney"       % Versions.chimney,
    "io.estatico"                 %% "newtype"       % Versions.newType,
    "com.softwaremill.sttp.tapir" %% "tapir-newtype" % Versions.tapir
  ) map (_ % Compile)

  lazy val dbDep: Seq[ModuleID] = Seq(
    "io.getquill"  %% "quill-jdbc"           % Versions.quill,
    "io.getquill"  %% "quill-jdbc-zio"       % Versions.quill,
    "com.beachape" %% "enumeratum"           % Versions.enumeratum,
    "com.beachape" %% "enumeratum-quill"     % Versions.enumeratum,
    "com.beachape" %% "enumeratum-circe"     % Versions.enumeratum,
    "mysql"         % "mysql-connector-java" % Versions.mysql,
    "org.flywaydb"  % "flyway-core"          % Versions.flyway,
    "org.flywaydb"  % "flyway-mysql"         % Versions.flyway
  ) map (_ % Compile)

  lazy val httpDep: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"      % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"   % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
    "dev.zio"                     %% "zio-http"                % Versions.zioHttp
  ) map (_ % Compile)

  lazy val logDep: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio-logging"              % Versions.zioLogging,
    "dev.zio" %% "zio-logging-slf4j-bridge" % Versions.zioLogging, // routes slf4j to zio-logging
    // with exclude reroutes other loggers (can be brought by other dependencies) to slf4j
    "org.slf4j" % "jcl-over-slf4j"   % Versions.slf4j,
    "org.slf4j" % "log4j-over-slf4j" % Versions.slf4j,
    "org.slf4j" % "jul-to-slf4j"     % Versions.slf4j
  ) map (_ % Compile)

  lazy val logExcludeDep: Seq[InclExclRule] = Seq(
    ExclusionRule("commons-logging", "commons-logging"),
    ExclusionRule("log4j", "log4j")
  )

  lazy val testDep: Seq[ModuleID] = Seq(
    "dev.zio"               %% "zio-test"                     % Versions.zio,
    "dev.zio"               %% "zio-test-sbt"                 % Versions.zio,
    "io.github.scottweaver" %% "zio-2-0-testcontainers-mysql" % Versions.zioTestContainers,
    "io.github.scottweaver" %% "zio-2-0-db-migration-aspect"  % Versions.zioTestContainers
  ) map (_ % Test)
}
