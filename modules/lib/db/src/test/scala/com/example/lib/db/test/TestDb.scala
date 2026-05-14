package com.example.lib.db.test

import java.util.UUID
import javax.sql.DataSource

import com.example.lib.common.domain.error.backend.DbError
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.service.TransactorQuillImpl
import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.test.TestDbIntrospection.ForeignKey
import zio._

/** Per-test MySQL schema isolation.
  *
  * Each test that uses `freshSchemaLayer` gets a schema named `test_<uuid>` cloned (structure only) from a pre-migrated
  * template, a `SqlContext` + `Transactor` pointing at it, and the schema dropped on scope close. The MySQL container
  * itself is brought up by the surrounding test infra (see `commands.md` for the recipe) — `TestDbConfig.fromEnv` picks
  * up host/port/user/password and the template schema name.
  */
object TestDb {

  /** Scoped layer: provisions a fresh MySQL schema, provides `SqlContext` pointing at it, drops schema on scope close.
    */
  private val freshContextLayer: ZLayer[Any, Throwable, SqlContext] =
    ZLayer.scoped {
      val cfg = TestDbConfig.fromEnv
      for {
        schemaName <- ZIO.succeed(s"test_${UUID.randomUUID().toString.replace("-", "_")}")
        setupDs    <- TestDbConfig.buildDataSource(cfg = cfg, schema = None)
        _          <- runStatementOn(ds = setupDs, sql = s"CREATE DATABASE `$schemaName`")
        // Finalizer runs LIFO before setupDs's own acquireRelease closes it, so setupDs is still alive here.
        _      <- ZIO.addFinalizer(dropSchema(ds = setupDs, schemaName = schemaName))
        _      <- cloneTables(ds = setupDs, source = cfg.templateSchema, target = schemaName)
        testDs <- TestDbConfig.buildDataSource(cfg = cfg, schema = Some(schemaName))
      } yield SqlContext(testDs)
    }

  /** Convenience: provisions fresh schema + provides both `SqlContext` and `Transactor`. */
  val freshSchemaLayer: ZLayer[Any, Throwable, Transactor & SqlContext] =
    freshContextLayer >+> TransactorQuillImpl.layer

  /** Run a side-effecting SQL statement against the active test schema. */
  def runSql(sql: String): ZIO[SqlContext, Throwable, Unit] =
    ZIO.serviceWithZIO[SqlContext](ctx => runStatementOn(ds = ctx.ds, sql = sql))

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

  /** Clone tables (structure + indexes via `CREATE TABLE LIKE`) and FKs (added separately — `LIKE` skips them). */
  private def cloneTables(ds: DataSource, source: String, target: String): Task[Unit] =
    for {
      tables <- listTables(ds = ds, schema = source)
      _ <- ZIO.foreachDiscard(tables)(t =>
        runStatementOn(ds = ds, sql = s"CREATE TABLE `$target`.`$t` LIKE `$source`.`$t`")
      )
      foreignKeys <- TestDbIntrospection.listForeignKeysInSchema(ds = ds, schema = source)
      _ <- ZIO.foreachDiscard(foreignKeys)(fk =>
        runStatementOn(ds = ds, sql = addForeignKeyStmt(target = target, fk = fk))
      )
    } yield ()

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

  private def dropSchema(ds: DataSource, schemaName: String): UIO[Unit] =
    runStatementOn(ds = ds, sql = s"DROP DATABASE IF EXISTS `$schemaName`")
      .tapErrorCause(c => ZIO.logErrorCause(s"Failed to drop test schema $schemaName", c))
      .ignore
}
