package org.organization.api.to

import java.time.Instant

import io.circe.generic.JsonCodec
import org.organization.api.model.NewType._
import org.organization.db.model._

@JsonCodec
final case class PersonTO(
    identifier: PersonIdentifier,
    name: String,
    birthDate: Instant,
    gender: Gender,
    isArchived: Boolean
)
