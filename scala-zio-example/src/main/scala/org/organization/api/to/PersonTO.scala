package org.organization.api.to

import java.time.Instant

import io.circe.generic.JsonCodec
import io.circe.generic.auto._
import org.organization.api.model.NewType.PersonIdentifier
import org.organization.db.model.Gender

@JsonCodec
final case class PersonTO(
    identifier: PersonIdentifier,
    name: String,
    birthDate: Instant,
    gender: Gender,
    isArchived: Boolean
)
