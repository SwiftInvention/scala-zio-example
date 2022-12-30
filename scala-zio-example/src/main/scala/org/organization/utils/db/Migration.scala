package org.organization.utils.db

import org.flywaydb.core.Flyway
import org.organization.AppEnv.AppRIO
import zio._

import javax.sql.DataSource

object Migration {
  def migrate: AppRIO[Has[DataSource], Unit] = for {
    ds <- ZIO.service[DataSource]
    _  <- ZIO.effectTotal(println("Start migrating the database"))

    _ <- ZIO.effect(
      Flyway.configure
        .locations("db/migration")
        .dataSource(ds)
        .baselineOnMigrate(true)
        .load
        .migrate()
    )

    _ <- ZIO.effectTotal(println("Migration successful"))
  } yield ()
}
