package com.example.lib.db.test

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zio._

/** Test-DB connection parameters plus the data-source builder shared by the per-schema setup path (`TestDb`) and the
  * template-introspection path (`TestDbIntrospection`).
  *
  * Connection params come from environment variables with local-dev defaults that match
  * `application-local.conf.example` — a developer who's run `just initial-setup` works out of the box. Defaults live in
  * code here, not in a typed conf file, because test bootstrap runs before any `ConfigBootstrap` plumbing.
  */
final case class TestDbConfig(
    host: String,
    port: Int,
    user: String,
    password: String,
    templateSchema: String,
    jdbcParams: String
) {
  def setupJdbcUrl: String                  = s"jdbc:mysql://$host:$port/?$jdbcParams"
  def schemaJdbcUrl(schema: String): String = s"jdbc:mysql://$host:$port/$schema?$jdbcParams"
}

object TestDbConfig {

  // Defaults match docker-compose.test.yml: port 3307 isolates the test container from the local dev MySQL on 3306;
  // user `root` because per-test schemas require CREATE/DROP DATABASE; `localPassword` matches MYSQL_ROOT_PASSWORD.
  //
  // styleguide: config-shape WONTFIX — `config-shape` bans `getOrElse` defaults in production code; test bootstrap is a
  // deliberately different concern that runs before `ConfigBootstrap` plumbing exists.
  def fromEnv: TestDbConfig = TestDbConfig(
    host = sys.env.getOrElse("TEST_DB_HOST", "localhost"),
    port = sys.env.get("TEST_DB_PORT").map(_.toInt).getOrElse(3307),
    user = sys.env.getOrElse("TEST_DB_USER", "root"),
    password = sys.env.getOrElse("TEST_DB_PASSWORD", "localPassword"),
    templateSchema = sys.env.getOrElse("TEST_DB_TEMPLATE", "localDatabase"),
    jdbcParams = "useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
  )

  /** Build a pooled `DataSource` scoped to the calling scope. `schema = None` connects without a default schema (for
    * `CREATE DATABASE` and information-schema queries); `Some(name)` connects directly to that schema.
    */
  def buildDataSource(cfg: TestDbConfig, schema: Option[String]): ZIO[Scope, Throwable, HikariDataSource] = {
    val acquire = ZIO.attempt {
      val hc = new HikariConfig()
      hc.setJdbcUrl(schema.fold(cfg.setupJdbcUrl)(cfg.schemaJdbcUrl))
      hc.setUsername(cfg.user)
      hc.setPassword(cfg.password)
      hc.setMaximumPoolSize(4)
      hc.validate()
      new HikariDataSource(hc)
    }
    val release = (ds: HikariDataSource) => ZIO.attempt(ds.close()).ignore
    ZIO.acquireRelease(acquire)(release)
  }
}
