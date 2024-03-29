package org.organization.utils

import java.time.Instant
import javax.sql.DataSource

import org.organization.AppEnv.AppRIO
import org.organization.db.model.{Gender, NewPersonData}
import org.organization.db.repository.PersonRepository

object DemoData extends PersonRepository {

  val fillDb: AppRIO[DataSource, Unit] =
    for {
      _ <- insert(
        NewPersonData(
          "PersonName 1",
          Instant.parse("2007-12-03T10:15:30.00Z"),
          Gender.Male
        )
      )
      _ <- insert(
        NewPersonData(
          "PersonName 2",
          Instant.parse("1984-01-02T10:10:50.00Z"),
          Gender.Female
        )
      )
      _ <- insert(
        NewPersonData(
          "PersonName 3",
          Instant.parse("1988-11-02T10:10:50.00Z"),
          Gender.NonBinary
        )
      )
    } yield ()
}
