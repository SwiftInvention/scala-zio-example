package org.organization.integration_tests

import java.time.Instant
import java.util.UUID
import javax.sql.DataSource

import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.model.Gender.{Female, Male, NonBinary}
import org.organization.db.model.NewPersonData
import org.organization.db.repository.PersonRepository
import org.organization.integration_tests.util.{DatabaseIntegrationSpec, SnapshotSpec}
import zio.test._

object PersonRepositorySpec extends DatabaseIntegrationSpec with SnapshotSpec with PersonRepository {

  def integrationSpec: Spec[DataSource, Throwable] =
    suite("PersonRepository")(
      test("can insert record") {
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
      },
      test("getOldest returns None if person table is empty") {
        assertZIO(getOldest)(Assertion.equalTo(None))
      },
      test("returns the oldest person") {
        val firstPerson =
          NewPersonData(
            name = "Ivan Petrova",
            birthDate = Instant.ofEpochSecond(42),
            gender = NonBinary
          )
        val secondPerson = NewPersonData(
          name = "John Doe",
          birthDate = Instant.ofEpochSecond(29),
          gender = Male
        )
        val thirdPerson = NewPersonData(
          name = "Barbara Liskov",
          birthDate = Instant.ofEpochSecond(47),
          gender = Female
        )
        for {
          _                     <- insert(firstPerson)
          createdSecondPersonId <- insert(secondPerson)
          _                     <- insert(thirdPerson)
          personFromDb          <- getOldest
        } yield assert(personFromDb.map(_.id))(Assertion.equalTo(Some(createdSecondPersonId)))
      },
      test("filters archived persons") {
        val archivedPerson = NewPersonData(
          name = "John Doe",
          birthDate = Instant.ofEpochSecond(29),
          gender = Male
        )
        val nonArchivedPerson =
          NewPersonData(
            name = "Ivan Petrova",
            birthDate = Instant.ofEpochSecond(42),
            gender = NonBinary
          )
        for {
          archivedPersonId <- insert(archivedPerson)
          _                <- insert(nonArchivedPerson)
          _                <- archive(archivedPersonId)
          allPersons       <- getAllPersons
          testUuid = PersonIdentifier.fromUUID(UUID.fromString("00000000-0000-0000-0000-000000000000"))
          allPersonsSnapshotOk <- matchesJsonSnapshot(
            "PersonRepository/allPersons.json",
            allPersons.map(_.toTO.copy(identifier = testUuid))
          )
          nonArchivedPersons <- getPersons
          nonArchivedPersonsSnapshotOk <- matchesJsonSnapshot(
            "PersonRepository/nonArchivedPersons.json",
            nonArchivedPersons.map(_.toTO.copy(identifier = testUuid))
          )
        } yield allPersonsSnapshotOk && nonArchivedPersonsSnapshotOk
      }
    )
}
