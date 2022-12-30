package org.organization.utils

import org.organization.AppEnv.AppRIO
import org.organization.db.model.{Gender, NewPersonData}
import org.organization.db.repository.PersonRepository
import zio.Has

import java.time.Instant
import javax.sql.DataSource

object DemoData extends PersonRepository {

  val fillDb: AppRIO[Has[DataSource], Unit] =
    for {
      _ <- insert(
        NewPersonData("PersonName 1", Instant.parse("2007-12-03T10:15:30.00Z"), Gender.Male)
      )
      _ <- insert(
        NewPersonData("PersonName 2", Instant.parse("1984-01-02T10:10:50.00Z"), Gender.Female)
      )
    } yield ()
}
