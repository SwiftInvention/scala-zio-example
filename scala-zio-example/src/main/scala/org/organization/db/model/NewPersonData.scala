package org.organization.db.model

import java.time.Instant

final case class NewPersonData(
    name: String,
    birthDate: Instant,
    gender: Gender
)
