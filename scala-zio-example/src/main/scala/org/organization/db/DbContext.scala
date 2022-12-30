package org.organization.db

import org.organization.db.model.PersonEnt
import io.getquill.{EntityQuery, MysqlZioJdbcContext, Quoted, SnakeCase}

object DbContext {
  lazy val ctx = new MysqlZioJdbcContext(SnakeCase)

  val person: Quoted[EntityQuery[PersonEnt]] = ctx.quote {
    ctx.querySchema[PersonEnt]("person")
  }
}
