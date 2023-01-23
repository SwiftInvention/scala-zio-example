import sbt._

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
    "com.beachape" %% "enumeratum-quill"     % Versions.enumeratum,
    "com.beachape" %% "enumeratum"           % Versions.enumeratum,
    "mysql"         % "mysql-connector-java" % Versions.mysql,
    "org.flywaydb"  % "flyway-core"          % Versions.flyway,
    "org.flywaydb"  % "flyway-mysql"         % Versions.flyway
  ) map (_ % Compile)

  lazy val httpDep: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"      % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio1-http-server"  % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
    "io.d11"                       % "zhttp_2.13"              % Versions.zioHttp
  ) map (_ % Compile)

  lazy val testDep: Seq[ModuleID] = Seq(
    "dev.zio"               %% "zio-test"                 % Versions.zio,
    "dev.zio"               %% "zio-test-sbt"             % Versions.zio,
    "io.github.scottweaver" %% "zio-testcontainers-mysql" % Versions.zioTestContainers,
    "io.github.scottweaver" %% "zio-db-migration-aspect"  % Versions.zioTestContainers
  ) map (_ % Test)
}
