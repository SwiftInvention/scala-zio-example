package com.example.lib.db.test

import javax.sql.DataSource

import com.example.lib.db.impl.sql.SqlContext
import zio._

/** Schema introspection helpers — list columns and foreign keys of a table from `information_schema`. Used by
  * meta-tests that compare a cloned schema against its template, and by `TestDb.cloneTables` when re-emitting FKs.
  */
object TestDbIntrospection {

  final case class Column(name: String, columnType: String, isNullable: String)

  final case class ForeignKey(
      constraintName: String,
      tableName: String,
      columns: List[String],
      referencedTable: String,
      referencedColumns: List[String],
      updateRule: String,
      deleteRule: String
  )

  /** List columns of `table` in the schema the `SqlContext` is bound to. */
  def listColumns(ctx: SqlContext, table: String): Task[List[Column]] =
    currentSchema(ctx.ds).flatMap(schema => listColumnsOn(ds = ctx.ds, schema = schema, table = table))

  /** List columns of `table` in the template schema — opens a short-lived connection. */
  def listColumnsInTemplate(table: String): Task[List[Column]] = {
    val cfg = TestDbConfig.fromEnv
    ZIO.scoped {
      TestDbConfig
        .buildDataSource(cfg = cfg, schema = None)
        .flatMap(ds => listColumnsOn(ds = ds, schema = cfg.templateSchema, table = table))
    }
  }

  /** List FKs of `table` in the schema the `SqlContext` is bound to. */
  def listForeignKeys(ctx: SqlContext, table: String): Task[List[ForeignKey]] =
    currentSchema(ctx.ds).flatMap(schema => listForeignKeysOn(ds = ctx.ds, schema = schema, table = Some(table)))

  /** List FKs of `table` in the template schema — opens a short-lived connection. */
  def listForeignKeysInTemplate(table: String): Task[List[ForeignKey]] = {
    val cfg = TestDbConfig.fromEnv
    ZIO.scoped {
      TestDbConfig
        .buildDataSource(cfg = cfg, schema = None)
        .flatMap(ds => listForeignKeysOn(ds = ds, schema = cfg.templateSchema, table = Some(table)))
    }
  }

  /** List every FK in `schema` (across all tables). Package-private — `TestDb.cloneTables` uses it to re-emit FKs after
    * `CREATE TABLE ... LIKE` (which doesn't carry them across).
    */
  private[test] def listForeignKeysInSchema(ds: DataSource, schema: String): Task[List[ForeignKey]] =
    listForeignKeysOn(ds = ds, schema = schema, table = None)

  private def currentSchema(ds: DataSource): Task[String] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val stmt = conn.createStatement()
        try {
          val rs = stmt.executeQuery("SELECT DATABASE()")
          try {
            rs.next()
            rs.getString(1)
          } finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  private def listColumnsOn(ds: DataSource, schema: String, table: String): Task[List[Column]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val stmt = conn.prepareStatement(
          """SELECT column_name, column_type, is_nullable FROM information_schema.columns
            |WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position""".stripMargin
        )
        try {
          stmt.setString(1, schema)
          stmt.setString(2, table)
          val rs = stmt.executeQuery()
          try
            Iterator
              .continually(rs.next())
              .takeWhile(identity)
              .map(_ => Column(name = rs.getString(1), columnType = rs.getString(2), isNullable = rs.getString(3)))
              .toList
          finally rs.close()
        } finally stmt.close()
      } finally conn.close()
    }

  private def listForeignKeysOn(ds: DataSource, schema: String, table: Option[String]): Task[List[ForeignKey]] =
    ZIO.attemptBlocking {
      val conn = ds.getConnection
      try {
        val baseSql =
          """SELECT rc.constraint_name, rc.table_name, kcu.column_name,
            |       rc.referenced_table_name, kcu.referenced_column_name, rc.update_rule, rc.delete_rule
            |FROM information_schema.referential_constraints rc
            |JOIN information_schema.key_column_usage kcu
            |  ON rc.constraint_schema = kcu.constraint_schema
            | AND rc.constraint_name   = kcu.constraint_name
            | AND rc.table_name        = kcu.table_name
            |WHERE rc.constraint_schema = ?""".stripMargin
        val sql = table match {
          case Some(_) => s"$baseSql AND rc.table_name = ? ORDER BY rc.constraint_name, kcu.ordinal_position"
          case None    => s"$baseSql ORDER BY rc.table_name, rc.constraint_name, kcu.ordinal_position"
        }
        val stmt = conn.prepareStatement(sql)
        try {
          stmt.setString(1, schema)
          table.foreach(stmt.setString(2, _))
          val rs = stmt.executeQuery()
          try {
            val raw = Iterator
              .continually(rs.next())
              .takeWhile(identity)
              .map { _ =>
                (
                  rs.getString(1),
                  rs.getString(2),
                  rs.getString(3),
                  rs.getString(4),
                  rs.getString(5),
                  rs.getString(6),
                  rs.getString(7)
                )
              }
              .toList
            raw
              .groupBy { case (cn, tn, _, _, _, _, _) => (cn, tn) }
              .toList
              .sortBy { case ((cn, tn), _) => (tn, cn) }
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
}
