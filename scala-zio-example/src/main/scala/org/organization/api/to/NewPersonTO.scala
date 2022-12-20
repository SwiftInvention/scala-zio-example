package org.organization.api.to

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.db.model.{Gender, NewPersonData}

import java.time.Instant

final case class NewPersonTO(name: String, birthDate: Instant, gender: Gender) {
  def toDomain(): NewPersonData =
    this.into[NewPersonData].transform
}
