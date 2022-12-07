package org.organization.api.to

import org.organization.db.model.Gender

import java.time.Instant
import java.util.UUID

final case class PersonTO(identifier: UUID, name: String, birthDate: Instant, gender: Gender)
