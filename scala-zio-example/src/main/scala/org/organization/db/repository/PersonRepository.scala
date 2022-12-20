package org.organization.db.repository

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.AppEnv.AppIO
import org.organization.db.DbContext._
import org.organization.db.DbContext.ctx._
import org.organization.db.model.{NewPersonData, PersonEnt}

import java.util.UUID

trait PersonRepository {

  def getAllPersons: AppIO[List[PersonEnt]] = {
    val q = ctx.quote {
      person.sortBy(_.birthDate)
    }
    run(q)
  }

  def getById(id: Long): AppIO[Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.id equals lift(id))
    }
    run(q).map(_.headOption)
  }

  def getByIdentifier(identifier: UUID): AppIO[Option[PersonEnt]] = {
    val q = ctx.quote {
      person.filter(_.identifier equals lift(identifier))
    }
    run(q).map(_.headOption)
  }

  def insert(newPersonData: NewPersonData): AppIO[Long] = {
    val stubId: Long = 0
    val uuid         = UUID.randomUUID()
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
