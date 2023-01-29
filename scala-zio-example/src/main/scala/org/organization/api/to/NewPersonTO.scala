package org.organization.api.to

import java.time.Instant

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.db.model.{Gender, NewPersonData}

final case class NewPersonTO(name: String, birthDate: Instant, gender: Gender) {
  def toDomain: NewPersonData =
    this.into[NewPersonData].transform
}
