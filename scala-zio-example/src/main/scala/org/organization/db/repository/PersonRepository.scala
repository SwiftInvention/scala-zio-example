package org.organization.db.repository

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.AppEnv.AppRIO
import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.DbContext._
import org.organization.db.DbContext.ctx._
import org.organization.db.model.{NewPersonData, PersonEnt}
import zio.Has

import java.util.UUID
import javax.sql.DataSource

trait PersonRepository {

  implicit val encodeIdentifier: MappedEncoding[PersonIdentifier, UUID] =
    MappedEncoding[PersonIdentifier, UUID](_.value)
  implicit val decodeIdentifier: MappedEncoding[UUID, PersonIdentifier] =
    MappedEncoding[UUID, PersonIdentifier](PersonIdentifier.fromUUID)

  def getAllPersons: AppRIO[Has[DataSource], List[PersonEnt]] = {
    val q = ctx.quote {
      person.sortBy(_.birthDate)
    }
    run(q)
  }

  def getById(id: Long): AppRIO[Has[DataSource], Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.id equals lift(id))
    }
    run(q).map(_.headOption)
  }

  def getByIdentifier(identifier: PersonIdentifier): AppRIO[Has[DataSource], Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.identifier equals lift(identifier))
    }
    run(q).map(_.headOption)
  }

  def insert(newPersonData: NewPersonData): AppRIO[Has[DataSource], Long] = {
    val stubId: Long = 0
    val uuid         = PersonIdentifier.fromUUID(UUID.randomUUID())
    val personEntToCreate = newPersonData
      .into[PersonEnt]
      .withFieldConst(_.id, stubId)
      .withFieldConst(_.identifier, uuid)
      .transform
    val q = ctx.quote {
      person.insertValue(lift(personEntToCreate)).returningGenerated(_.id)
    }
    run(q)
  }
}
