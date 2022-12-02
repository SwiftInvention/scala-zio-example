package org.organization.db.repository

import org.organization.db.DbContext.ctx._
import org.organization.db.DbContext._
import org.organization.db.model.PersonEnt
import org.organization.AppEnv.AppIO

trait PersonRepository {
  def getAllPersons: AppIO[List[PersonEnt]] = {
    val q = ctx.quote {
      person.sortBy(_.birthDate)
    }
    run(q)
  }
}
