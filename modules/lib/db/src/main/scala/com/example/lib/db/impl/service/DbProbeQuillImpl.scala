package com.example.lib.db.impl.service

import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.common.domain.service.DbProbe
import com.example.lib.db.impl.sql.SqlContext
import zio._

/** `DbProbe` against the SQL datasource. A single-round-trip `SELECT 1`, with no surrounding transaction.
  *
  * `.disconnect` forks the query to a separate fiber so a caller's `.timeout` can interrupt the wait — Quill's effect
  * is uninterruptible, so without it the timeout would block on the full JDBC `connectionTimeout`.
  */
final class DbProbeQuillImpl(ctx: SqlContext) extends DbProbe {
  import ctx._

  override val ping: AppIO[Unit] = {
    val q = quote(sql"""SELECT 1""".as[Int])
    ctx.runQuery(run(q)).unit.disconnect
  }
}

object DbProbeQuillImpl {
  val layer: URLayer[SqlContext, DbProbe] =
    ZLayer.fromFunction(new DbProbeQuillImpl(_))
}
