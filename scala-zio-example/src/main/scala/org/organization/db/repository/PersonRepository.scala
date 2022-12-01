package org.organization.db.repository

import org.organization.db.DbContext.ctx._
import org.organization.db.DbContext._
import org.organization.db.model.Person
import org.organization.AppEnv.AppIO

trait PersonRepository {
  def getAllPersons: AppIO[List[Person]] = {
    val q = ctx.quote {
      persons.sortBy(_.birthDate)
    }
    run(q)
  }
}
