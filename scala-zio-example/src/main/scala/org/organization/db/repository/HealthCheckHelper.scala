package org.organization.db.repository

import org.organization.db.DbContext.ctx._
import org.organization.db.DbContext._
import org.organization.AppEnv.AppIO

trait HealthCheckHelper {

  def databaseHealthCheck: AppIO[Unit] = {
    val q = ctx.quote {
      infix"""SELECT 1""".as[Int]
    }
    run(q).unit
  }
}