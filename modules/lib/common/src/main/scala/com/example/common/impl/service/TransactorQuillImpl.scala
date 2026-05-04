package com.example.common.impl.service

import com.example.common.domain.error.backend.DbError
import com.example.common.domain.model.Types.AppIO
import com.example.common.domain.service.Transactor
import com.example.common.impl.repo.pg.PgContext
import zio._

/** Quill-backed `Transactor`. Opens a JDBC transaction on the underlying
  * `DataSource` for the duration of the wrapped effect.
  *
  * On failure of the wrapped effect: rolls back, propagates the original error.
  * On unexpected SQL/JDBC exception: rolls back, fails with `DbError`.
  *
  * Quill's `ZioJdbcContext.transaction` is reentrant — a nested call within
  * the same fiber reuses the outer connection (no nested SQL transaction is
  * opened), so app services can wrap repo calls without breaking the inner
  * tx-per-method default.
  */
final class TransactorQuillImpl(ctx: PgContext) extends Transactor {

  override def withTransaction[A](io: AppIO[A]): AppIO[A] =
    ctx
      .transaction(io)
      .provideEnvironment(ZEnvironment(ctx.ds))
      .catchSome {
        // Translate raw SQL/JDBC exceptions; pass `AppFailure`s (incl. DbError) through.
        case e: java.sql.SQLException =>
          ZIO.fail(DbError(s"Transaction failed: ${e.getMessage}", Some(e)))
      }
}

object TransactorQuillImpl {
  val layer: URLayer[PgContext, Transactor] =
    ZLayer.fromFunction(new TransactorQuillImpl(_))
}
