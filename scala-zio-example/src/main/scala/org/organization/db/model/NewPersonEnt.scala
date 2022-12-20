package org.organization.db.model

import java.time.Instant

final case class NewPersonEnt(
    name: String,
    birthDate: Instant,
    gender: Gender
)
