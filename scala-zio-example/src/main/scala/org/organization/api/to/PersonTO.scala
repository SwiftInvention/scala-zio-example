package org.organization.api.to

import java.time.Instant

import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.model.Gender

final case class PersonTO(
    identifier: PersonIdentifier,
    name: String,
    birthDate: Instant,
    gender: Gender,
    isArchived: Boolean
)
