package org.organization.utils.db

import javax.sql.DataSource

import org.flywaydb.core.Flyway
import org.organization.AppEnv.AppRIO
import zio._

object Migration {
  def migrate: AppRIO[DataSource, Unit] = for {
    ds <- ZIO.service[DataSource]
    _  <- ZIO.logInfo("Start migrating the database")

    _ <- ZIO.attempt(
      Flyway.configure
        .locations("db/migration")
        .dataSource(ds)
        .baselineOnMigrate(true)
        .load
        .migrate()
    )

    _ <- ZIO.logInfo("Migration successful")
  } yield ()
}
