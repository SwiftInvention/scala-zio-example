package org.organization.api.to

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.db.model.{Gender, NewPersonEnt}

import java.time.Instant

final case class NewPersonTO(name: String, birthDate: Instant, gender: Gender) {
  def toEnt(): NewPersonEnt =
    this.into[NewPersonEnt].transform
}
