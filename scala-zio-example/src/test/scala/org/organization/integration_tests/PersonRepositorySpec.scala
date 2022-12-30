package org.organization.integration_tests

import org.organization.db.model.Gender.NonBinary
import org.organization.db.model.NewPersonData
import org.organization.db.repository.PersonRepository
import org.organization.integration_tests.util.DatabaseIntegrationSpec
import zio.Has
import zio.test._

import java.time.Instant
import javax.sql.DataSource

object PersonRepositorySpec extends DatabaseIntegrationSpec with PersonRepository {

  def integrationSpec: ZSpec[Has[DataSource], Throwable] =
    suite("PersonRepository")(
      testM("can insert record") {
        val newPerson =
          NewPersonData(
            name = "Ivan Petrova",
            birthDate = Instant.ofEpochSecond(42),
            gender = NonBinary
          )
        for {
          createdPersonId <- insert(newPerson)
          personFromDb    <- getById(createdPersonId)
        } yield (
          assert(personFromDb.map(_.name))(Assertion.equalTo(Some(newPerson.name)))
            && assert(personFromDb.map(_.birthDate))(Assertion.equalTo(Some(newPerson.birthDate)))
            && assert(personFromDb.map(_.gender))(Assertion.equalTo(Some(newPerson.gender)))
        )
      }
    )
}
