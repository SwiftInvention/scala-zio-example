package org.organization.db.repository

import javax.sql.DataSource

import org.organization.AppEnv.AppRIO
import org.organization.db.DbContext._
import org.organization.db.DbContext.ctx._

trait HealthCheckHelper {

  def databaseHealthCheck: AppRIO[DataSource, Unit] = {
    val q = ctx.quote {
      sql"""SELECT 1""".as[Int]
    }

    /** Note: the quill effect is uninterruptible which means that if the database is unavailable, then all requests to
      * the database (even the healthcheck) will wait for the default JDBC `connectionTimeout` to complete.
      *
      * And we probably don't want to set the `connectionTimeout` too small.
      *
      * So in order to make zio-http's `Middleware.timeout` work, we could run the healthcheck query in the global
      * scope, and let it quietly fail some time after the HTTP request is rejected.
      *
      * Which is a leak.
      *
      * Quadratisch. Praktisch. Gut.
      */
    run(q).unit.disconnect
  }
}
