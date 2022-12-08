package org.organization.db.repository

import org.organization.db.DbContext.ctx._
import org.organization.db.DbContext._
import org.organization.db.model.PersonEnt
import org.organization.AppEnv.AppIO

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

  def insert(productEnt: PersonEnt): AppIO[Long] = {
    val q = ctx.quote {
      person.insertValue(lift(productEnt)).returningGenerated(_.id)
    }
    run(q)
  }
}
