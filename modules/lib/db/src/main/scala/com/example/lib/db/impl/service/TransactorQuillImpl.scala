package com.example.lib.db.impl.service

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.error.backend.{DbError, InternalServerError}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.sql.SqlContext
import zio._

/** Quill-backed `Transactor`. Opens a JDBC transaction on the underlying `DataSource` for the duration of the wrapped
  * effect; rolls back on failure.
  *
  * Quill's `ZioJdbcContext.transaction` widens the error channel to `Throwable` (raw `SQLException`s can surface from
  * the driver). The `mapError` here narrows back to `AppFailure`: `AppFailure`s pass through, SQL exceptions become
  * `DbError`, any other `Throwable` becomes `InternalServerError`.
  */
final class TransactorQuillImpl(ctx: SqlContext) extends Transactor {

  override def withTransaction[A](io: AppIO[A]): AppIO[A] =
    ctx
      .transaction(io)
      .provideEnvironment(ZEnvironment(ctx.ds))
      .mapError {
        case f: AppFailure            => f
        case e: java.sql.SQLException => DbError(s"Transaction failed: ${e.getMessage}", Some(e))
        case other => InternalServerError(s"Unexpected error in transaction: ${other.getMessage}", Some(other))
      }
}

object TransactorQuillImpl {
  val layer: URLayer[SqlContext, Transactor] =
    ZLayer.fromFunction(new TransactorQuillImpl(_))
}
