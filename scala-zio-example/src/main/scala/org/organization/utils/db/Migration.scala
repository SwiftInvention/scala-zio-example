package org.organization.utils.db

import org.flywaydb.core.Flyway
import org.organization.AppEnv.AppRIO
import zio._

import javax.sql.DataSource

object Migration {
  def migrate: AppRIO[DataSource, Unit] = for {
    ds <- ZIO.service[DataSource]
    _  <- ZIO.succeed(println("Start migrating the database"))

    _ <- ZIO.attempt(
      Flyway.configure
        .locations("db/migration")
        .dataSource(ds)
        .baselineOnMigrate(true)
        .load
        .migrate()
    )

    _ <- ZIO.succeed(println("Migration successful"))
  } yield ()
}
