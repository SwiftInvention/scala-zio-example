package org.organization.db.model

import org.organization.api.to.PersonTO
import java.time.Instant
import java.util.UUID

final case class PersonEnt(id: Long, identifier: UUID, name: String, birthDate: Instant) {
  def toTO(): PersonTO =
    PersonTO(
      identifier = identifier,
      name = name,
      birthDate = birthDate
    )
}
