package org.organization.utils.db

import org.organization.AppEnv.AppIO
import org.flywaydb.core.Flyway
import zio._
import javax.sql.DataSource

object Migration {
  def migrate: AppIO[Unit] = for {
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
