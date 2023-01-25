package org.organization.db

import io.getquill.{EntityQuery, MysqlZioJdbcContext, Quoted, SnakeCase}
import org.organization.db.model.PersonEnt

object DbContext {
  lazy val ctx = new MysqlZioJdbcContext(SnakeCase)

  val person: Quoted[EntityQuery[PersonEnt]] = ctx.quote {
    ctx.querySchema[PersonEnt]("person")
  }
}
