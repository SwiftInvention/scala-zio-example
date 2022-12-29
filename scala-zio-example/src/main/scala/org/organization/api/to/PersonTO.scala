package org.organization.api.to

import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.model.Gender

import java.time.Instant

final case class PersonTO(
    identifier: PersonIdentifier,
    name: String,
    birthDate: Instant,
    gender: Gender
)
