package com.example.common.impl.repo.pg

import javax.sql.DataSource

import com.example.common.domain.error.backend.DbError
import com.example.common.domain.model.Types._
import io.getquill._
import io.getquill.context.ZioJdbc.QIO
import zio._

/** Quill MySQL JDBC context with cross-cutting encodings mixed in.
  *
  * Repos take a `PgContext` and import its members (`import ctx._`) to use Quill
  * DSL. The context owns query execution but not transaction boundaries — see
  * `Transactor` for that.
  *
  * Naming note: `PgContext` is a deliberate carry-over from prior projects that
  * used Postgres. Keeping the name decouples ctx-impl code from the underlying
  * SQL dialect — swapping to Postgres later is a one-line change here.
  */
final case class PgContext(ds: DataSource) extends MysqlZioJdbcContext(SnakeCase) with NewTypeEncodings {

  /** Runs a single query; SQL exceptions become `DbError`. */
  def runQuery[A](q: => QIO[A]): AppIO[A] =
    q.provideEnvironment(ZEnvironment(ds))
      .mapError(e => DbError(s"Database query failed: ${e.getMessage}", Some(e)))

  /** Runs a single query expected to return at most one row. */
  def runQuerySingleResult[A](q: => QIO[List[A]]): AppIO[Option[A]] =
    runQuery(q).map(_.headOption)
}

object PgContext {

  val layer: URLayer[DataSource, PgContext] =
    ZLayer.fromFunction(PgContext(_))
}
