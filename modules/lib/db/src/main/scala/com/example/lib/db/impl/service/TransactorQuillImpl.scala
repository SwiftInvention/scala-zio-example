package com.example.lib.db.impl.service

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.error.backend.{DbError, InternalServerError}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.repo.sql.SqlContext
import zio._

/** Quill-backed `Transactor`. Opens a JDBC transaction on the underlying `DataSource` for the duration of the wrapped
  * effect.
  *
  * On failure of the wrapped effect: rolls back, propagates the original `AppFailure`. On unexpected SQL/JDBC exception
  * raised by the JDBC driver itself: rolls back, fails with `DbError`.
  *
  * Quill's `ZioJdbcContext.transaction` widens the error channel to `Throwable` (it can raise raw `SQLException`s from
  * the driver). We narrow back to `AppFailure` here with a total `mapError`: any `AppFailure` from the wrapped effect
  * passes through unchanged; any non-`AppFailure` is wrapped (SQL exceptions as `DbError`, anything else as
  * `InternalServerError` — should be unreachable in practice, but the match must be total).
  *
  * Quill's `transaction` is reentrant — a nested call within the same fiber reuses the outer connection (no nested SQL
  * transaction is opened), so app services can wrap repo calls without breaking the inner tx-per-method default.
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
