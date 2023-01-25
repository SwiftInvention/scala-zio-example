package org.organization.db.repository

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.AppEnv.AppRIO
import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.DbContext._
import org.organization.db.DbContext.ctx._
import org.organization.db.model.{NewPersonData, PersonEnt}

import java.util.UUID
import javax.sql.DataSource

trait PersonRepository {

  implicit val encodeIdentifier: MappedEncoding[PersonIdentifier, UUID] =
    MappedEncoding[PersonIdentifier, UUID](_.value)
  implicit val decodeIdentifier: MappedEncoding[UUID, PersonIdentifier] =
    MappedEncoding[UUID, PersonIdentifier](PersonIdentifier.fromUUID)

  def getPersons: AppRIO[DataSource, List[PersonEnt]] = {
    run(ctx.quote { person.sortBy(_.birthDate).filter(!_.isArchived) })
  }

  def getAllPersons: AppRIO[DataSource, List[PersonEnt]] = {
    run(ctx.quote { person.sortBy(_.birthDate) })
  }

  def getById(id: Long): AppRIO[DataSource, Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.id equals lift(id))
    }
    run(q).map(_.headOption)
  }

  def getByIdentifier(identifier: PersonIdentifier): AppRIO[DataSource, Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.identifier equals lift(identifier))
    }
    run(q).map(_.headOption)
  }

  def getOldest: AppRIO[DataSource, Option[PersonEnt]] = {
    val q = ctx.quote {
      person.sortBy(_.birthDate).take(1)
    }
    run(q).map(_.headOption)
  }

  def archive(id: Long): AppRIO[DataSource, Long] = {
    run(ctx.quote {
      person.filter(_.id equals lift(id)).update(_.isArchived -> lift(true))
    })
  }

  def insert(newPersonData: NewPersonData): AppRIO[DataSource, Long] = {
    val stubId: Long = 0
    val uuid         = PersonIdentifier.fromUUID(UUID.randomUUID())
    val personEntToCreate = newPersonData
      .into[PersonEnt]
      .withFieldConst(_.id, stubId)
      .withFieldConst(_.identifier, uuid)
      .withFieldConst(_.isArchived, false)
      .transform
    val q = ctx.quote {
      person.insertValue(lift(personEntToCreate)).returningGenerated(_.id)
    }
    run(q)
  }
}
