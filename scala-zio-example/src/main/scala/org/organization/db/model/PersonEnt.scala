package org.organization.db.model

import java.time.Instant

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.api.model.NewType.PersonIdentifier
import org.organization.api.to.PersonTO

final case class PersonEnt(
    id: Long,
    identifier: PersonIdentifier,
    name: String,
    birthDate: Instant,
    gender: Gender,
    isArchived: Boolean
) {
  def toTO: PersonTO = this.into[PersonTO].transform
}
