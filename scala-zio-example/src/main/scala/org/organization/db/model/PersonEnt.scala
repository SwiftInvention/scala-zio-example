package org.organization.db.model

import io.scalaland.chimney.dsl.TransformerOps
import org.organization.api.to.PersonTO
import java.time.Instant
import java.util.UUID

final case class PersonEnt(
    id: Long,
    identifier: UUID,
    name: String,
    birthDate: Instant,
    gender: Gender
) {
  def toTO(): PersonTO =
    this.into[PersonTO].transform
}
