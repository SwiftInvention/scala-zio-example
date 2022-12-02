package org.organization.db

import org.organization.db.model.PersonEnt
import io.getquill.{MysqlZioJdbcContext, SnakeCase}

object DbContext {
  lazy val ctx = new MysqlZioJdbcContext(SnakeCase)

  val person = ctx.quote {
    ctx.querySchema[PersonEnt]("person")
  }
}
