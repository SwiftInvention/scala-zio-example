import sbt._

object Dependencies {

  lazy val commonDep = Seq(
    "dev.zio"               %% "zio"        % Versions.zio,
    "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
  ) map (_                   % Compile)

  lazy val dbDep = Seq(
    "io.getquill" %% "quill-jdbc"           % Versions.quill,
    "io.getquill" %% "quill-jdbc-zio"       % Versions.quill,
    "mysql"        % "mysql-connector-java" % Versions.mysql,
    "org.flywaydb" % "flyway-core"          % Versions.flyway,
    "org.flywaydb" % "flyway-mysql"         % Versions.flyway
  ) map (_         % Compile)

  lazy val httpDep = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core"              % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"      % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-zio1-http-server"  % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
    "io.d11"                       % "zhttp_2.13"              % Versions.zioHttpVersion
  ) map (_                         % Compile)

  lazy val testDep = Seq(
    "dev.zio" %% "zio-test"     % Versions.zio,
    "dev.zio" %% "zio-test-sbt" % Versions.zio
  ) map (_     % Test)
}
