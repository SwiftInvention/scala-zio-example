package com.example.common.test

import java.util.UUID
import javax.sql.DataSource

import com.example.common.domain.error.backend.DbError
import com.example.common.domain.service.Transactor
import com.example.common.impl.repo.sql.SqlContext
import com.example.common.impl.service.TransactorQuillImpl
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import zio._

/** Per-test MySQL schema isolation.
  *
  * Strategy: the test infra runs a single MySQL container for the whole test process — started outside the JVM, e.g.
  * via `just db-up`. Per-test isolation is a fresh *schema* (MySQL "schema" = "database"), cloned from a pre-migrated
  * template.
  *
  * Each test that uses `freshSchemaLayer` gets:
  *   - a unique schema named `test_<uuid>`
  *   - tables cloned (structure only) from the template via `CREATE TABLE ... LIKE`
  *   - a fresh `Transactor` + `SqlContext` pointing at the schema
  *   - the schema dropped on layer scope close
  *
  * Resource-cheap, parallel-safe (each test owns its namespace), and avoids the JVM bringing up infrastructure.
  *
  * Assumptions:
  *   - MySQL is reachable at `TestDbConfig.host:port`
  *   - The template schema (`TestDbConfig.templateSchema`) exists and is migrated. `just test-it` handles both (via
  *     `db-reset-test` and `db-migrate-test`); when invoking sbt directly, run those recipes first.
  *
  * Connection params come from environment variables with local-dev defaults — see `TestDbConfig.fromEnv`. These
  * defaults match `application-local.conf.example` so a developer who's run `just initial-setup` works out of the box.
  * Defaults here are deliberate: this is test bootstrapping, not production runtime config (so the `config-shape`
  * principle doesn't apply).
  */
object TestDb {

  final case class TestDbConfig(
      host: String,
      port: Int,
      user: String,
      password: String,
      templateSchema: String,
      jdbcParams: String
  ) {
    def setupJdbcUrl: String =
      s"jdbc:mysql://$host:$port/?$jdbcParams"
    def schemaJdbcUrl(schema: String): String =
      s"jdbc:mysql://$host:$port/$schema?$jdbcParams"
  }

  object TestDbConfig {
    // Default port `3307` points at the dedicated test mysql container (see docker-compose.test.yml).
    // This isolates integration test runs from the local dev MySQL on 3306 — `just db-reset` and
    // a running local server can't interfere with tests, and vice versa.
    //
    // Default user `root` because per-test schemas require CREATE/DROP DATABASE privileges. The
    // local docker-compose's MYSQL_ROOT_PASSWORD matches `localPassword`, so root + localPassword
    // works out of the box. In CI / shared environments, override via env vars.
    def fromEnv: TestDbConfig = TestDbConfig(
      host = sys.env.getOrElse("TEST_DB_HOST", "localhost"),
      port = sys.env.get("TEST_DB_PORT").map(_.toInt).getOrElse(3307),
      user = sys.env.getOrElse("TEST_DB_USER", "root"),
      password = sys.env.getOrElse("TEST_DB_PASSWORD", "localPassword"),
      templateSchema = sys.env.getOrElse("TEST_DB_TEMPLATE", "localDatabase"),
      jdbcParams = "useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true"
    )
  }

  /** Scoped layer: provisions a fresh MySQL schema, provides `SqlContext` pointing at it, drops schema on scope close.
    */
  private val freshContextLayer: ZLayer[Any, Throwable, SqlContext] =
    ZLayer.scoped {
      val cfg = TestDbConfig.fromEnv
      for {
        schemaName <- ZIO.succeed(s"test_${UUID.randomUUID().toString.replace("-", "_")}")
        setupDs    <- buildDataSource(cfg = cfg, schema = None)
        _          <- runStatementOn(ds = setupDs, sql = s"CREATE DATABASE `$schemaName`")
        // Finalizer runs LIFO before setupDs's own acquireRelease closes it, so setupDs is still alive here.
        _      <- ZIO.addFinalizer(dropSchema(ds = setupDs, schemaName = schemaName))
        _      <- cloneTables(ds = setupDs, source = cfg.templateSchema, target = schemaName)
        testDs <- buildDataSource(cfg = cfg, schema = Some(schemaName))
      } yield SqlContext(testDs)
    }

  /** Convenience: provisions fresh schema + provides both `SqlContext` and `Transactor`. */
  val freshSchemaLayer: ZLayer[Any, Throwable, Transactor & SqlContext] =
    freshContextLayer >+> TransactorQuillImpl.layer

  /** Run a side-effecting SQL statement against the active test schema. */
  def runSql(sql: String): ZIO[SqlContext, Throwable, Unit] =
    ZIO.serviceWithZIO[SqlContext](ctx => runStatementOn(ds = ctx.ds, sql = sql))

  /** A column descriptor for meta-test comparisons. */
  final case class Column(name: String, columnType: String, isNullable: String)

  /** List columns of `table` in the *current* database (the one the connection is bound to). */
  def listColumns(ctx: SqlContext, table: String): Task[List[Column]] =
    listColumnsOn(ds = ctx.ds, schema = None, table = table)

  /** List columns of `table` in the template schema — uses a separate short-lived connection. */
  def listColumnsInTemplate(table: String): Task[List[Column]] = {
    val cfg = TestDbConfig.fromEnv
    ZIO.scoped {
      for {
        ds   <- buildDataSource(cfg = cfg, schema = None)
        cols <- listColumnsOn(ds = ds, schema = Some(cfg.templateSchema), table = table)
      } yield cols
    }
  }

  private def listColumnsOn(ds: DataSource, schema: Option[String], table: String): Task[List[Column]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val sql = schema.fold(
          """SELECT column_name, column_type, is_nullable FROM information_schema.columns
            |WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position""".stripMargin
        )(_ => """SELECT column_name, column_type, is_nullable FROM information_schema.columns
                 |WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position""".stripMargin)
        val stmt = conn.prepareStatement(sql)
        try {
          schema match {
            case Some(s) =>
              stmt.setString(1, s)
              stmt.setString(2, table)
            case None =>
              stmt.setString(1, table)
          }
          val rs = stmt.executeQuery()
          try
            Iterator
              .continually(rs.next())
              .takeWhile(identity)
              .map(_ => Column(rs.getString(1), rs.getString(2), rs.getString(3)))
              .toList
          finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  /** A foreign-key descriptor for meta-test comparisons. */
  final case class ForeignKeyInfo(
      constraintName: String,
      tableName: String,
      columns: List[String],
      referencedTable: String,
      referencedColumns: List[String]
  )

  /** List FKs of `table` in the *current* database. */
  def listForeignKeys(ctx: SqlContext, table: String): Task[List[ForeignKeyInfo]] =
    listForeignKeysOn(ds = ctx.ds, schema = None, table = table)

  /** List FKs of `table` in the template schema — uses a separate short-lived connection. */
  def listForeignKeysInTemplate(table: String): Task[List[ForeignKeyInfo]] = {
    val cfg = TestDbConfig.fromEnv
    ZIO.scoped {
      for {
        ds  <- buildDataSource(cfg = cfg, schema = None)
        fks <- listForeignKeysOn(ds = ds, schema = Some(cfg.templateSchema), table = table)
      } yield fks
    }
  }

  private def listForeignKeysOn(
      ds: DataSource,
      schema: Option[String],
      table: String
  ): Task[List[ForeignKeyInfo]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val sql = schema.fold(
          """SELECT rc.constraint_name, rc.table_name, kcu.column_name,
            |       rc.referenced_table_name, kcu.referenced_column_name
            |FROM information_schema.referential_constraints rc
            |JOIN information_schema.key_column_usage kcu
            |  ON rc.constraint_schema = kcu.constraint_schema
            | AND rc.constraint_name   = kcu.constraint_name
            | AND rc.table_name        = kcu.table_name
            |WHERE rc.constraint_schema = DATABASE() AND rc.table_name = ?
            |ORDER BY rc.constraint_name, kcu.ordinal_position""".stripMargin
        )(_ => """SELECT rc.constraint_name, rc.table_name, kcu.column_name,
                 |       rc.referenced_table_name, kcu.referenced_column_name
                 |FROM information_schema.referential_constraints rc
                 |JOIN information_schema.key_column_usage kcu
                 |  ON rc.constraint_schema = kcu.constraint_schema
                 | AND rc.constraint_name   = kcu.constraint_name
                 | AND rc.table_name        = kcu.table_name
                 |WHERE rc.constraint_schema = ? AND rc.table_name = ?
                 |ORDER BY rc.constraint_name, kcu.ordinal_position""".stripMargin)
        val stmt = conn.prepareStatement(sql)
        try {
          schema match {
            case Some(s) =>
              stmt.setString(1, s)
              stmt.setString(2, table)
            case None =>
              stmt.setString(1, table)
          }
          val rs = stmt.executeQuery()
          try {
            val raw = Iterator
              .continually(rs.next())
              .takeWhile(identity)
              .map(_ => (rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)))
              .toList
            raw
              .groupBy { case (cn, tn, _, _, _) => (cn, tn) }
              .toList
              .sortBy { case ((cn, _), _) => cn }
              .map { case ((cn, tn), rows) =>
                ForeignKeyInfo(
                  constraintName = cn,
                  tableName = tn,
                  columns = rows.map(_._3),
                  referencedTable = rows.head._4,
                  referencedColumns = rows.map(_._5)
                )
              }
          } finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  // --- internals ---

  private def buildDataSource(cfg: TestDbConfig, schema: Option[String]): ZIO[Scope, Throwable, HikariDataSource] = {
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

  private def runStatementOn(ds: DataSource, sql: String): Task[Unit] =
    ZIO
      .attemptBlocking {
        val conn = ds.getConnection
        try {
          val stmt = conn.createStatement()
          try {
            val _ = stmt.execute(sql)
          } finally stmt.close()
        } finally conn.close()
      }
      .mapError(e => DbError(message = s"Test SQL failed: $sql — ${e.getMessage}", cause = Some(e)))

  private def listTables(ds: DataSource, schema: String): Task[List[String]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val stmt = conn.prepareStatement(
          "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'"
        )
        try {
          stmt.setString(1, schema)
          val rs = stmt.executeQuery()
          try
            Iterator.continually(rs.next()).takeWhile(identity).map(_ => rs.getString(1)).toList
          finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  private final case class ForeignKey(
      constraintName: String,
      tableName: String,
      columns: List[String],
      referencedTable: String,
      referencedColumns: List[String],
      updateRule: String,
      deleteRule: String
  )

  /** Lists FK rows (one per column, ordered) from `information_schema`. Caller groups by constraint. */
  private def listForeignKeyRows(
      ds: DataSource,
      schema: String
  ): Task[List[ForeignKey]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val stmt = conn.prepareStatement(
          """SELECT rc.constraint_name, rc.table_name, kcu.column_name, kcu.ordinal_position,
            |       rc.referenced_table_name, kcu.referenced_column_name, rc.update_rule, rc.delete_rule
            |FROM information_schema.referential_constraints rc
            |JOIN information_schema.key_column_usage kcu
            |  ON rc.constraint_schema = kcu.constraint_schema
            | AND rc.constraint_name   = kcu.constraint_name
            | AND rc.table_name        = kcu.table_name
            |WHERE rc.constraint_schema = ?
            |ORDER BY rc.table_name, rc.constraint_name, kcu.ordinal_position""".stripMargin
        )
        try {
          stmt.setString(1, schema)
          val rs = stmt.executeQuery()
          try {
            // Collect raw rows, then group into composite FKs preserving column order.
            val raw = Iterator
              .continually(rs.next())
              .takeWhile(identity)
              .map { _ =>
                (
                  rs.getString("constraint_name"),
                  rs.getString("table_name"),
                  rs.getString("column_name"),
                  rs.getString("referenced_table_name"),
                  rs.getString("referenced_column_name"),
                  rs.getString("update_rule"),
                  rs.getString("delete_rule")
                )
              }
              .toList
            raw
              .groupBy { case (cn, tn, _, _, _, _, _) => (cn, tn) }
              .toList
              .map { case ((cn, tn), rows) =>
                ForeignKey(
                  constraintName = cn,
                  tableName = tn,
                  columns = rows.map(_._3),
                  referencedTable = rows.head._4,
                  referencedColumns = rows.map(_._5),
                  updateRule = rows.head._6,
                  deleteRule = rows.head._7
                )
              }
          } finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  private def addForeignKeyStmt(target: String, fk: ForeignKey): String = {
    val cols    = fk.columns.map(c => s"`$c`").mkString(", ")
    val refCols = fk.referencedColumns.map(c => s"`$c`").mkString(", ")
    s"""ALTER TABLE `$target`.`${fk.tableName}`
       |  ADD CONSTRAINT `${fk.constraintName}`
       |  FOREIGN KEY ($cols)
       |  REFERENCES `$target`.`${fk.referencedTable}` ($refCols)
       |  ON UPDATE ${fk.updateRule}
       |  ON DELETE ${fk.deleteRule}""".stripMargin
  }

  /** Clone tables (structure + indexes via `CREATE TABLE LIKE`) and FKs (added separately — `LIKE` skips them). */
  private def cloneTables(ds: DataSource, source: String, target: String): Task[Unit] =
    for {
      tables <- listTables(ds = ds, schema = source)
      _ <- ZIO.foreachDiscard(tables)(t =>
        runStatementOn(ds = ds, sql = s"CREATE TABLE `$target`.`$t` LIKE `$source`.`$t`")
      )
      foreignKeys <- listForeignKeyRows(ds = ds, schema = source)
      _ <- ZIO.foreachDiscard(foreignKeys)(fk =>
        runStatementOn(ds = ds, sql = addForeignKeyStmt(target = target, fk = fk))
      )
    } yield ()

  private def dropSchema(ds: DataSource, schemaName: String): UIO[Unit] =
    runStatementOn(ds = ds, sql = s"DROP DATABASE IF EXISTS `$schemaName`")
      .tapErrorCause(c => ZIO.logErrorCause(s"Failed to drop test schema $schemaName", c))
      .ignore
}
