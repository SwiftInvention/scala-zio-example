package org.organization.utils

import org.organization.AppEnv.AppIO
import org.organization.db.model.{Gender, NewPersonEnt}
import org.organization.db.repository.PersonRepository

import java.time.Instant

object DemoData extends PersonRepository {


  val fillDb: AppIO[Unit] =
    for {
      _ <- insert(NewPersonEnt("PersonName 2", Instant.parse("1984-01-02T10:10:50.00Z"), Gender.Female))
      _ <- insert(NewPersonEnt("PersonName 1", Instant.parse("2007-12-03T10:15:30.00Z"), Gender.Male))
    } yield ()
}
