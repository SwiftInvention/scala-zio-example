package org.organization.db.model

import java.time.Instant
import java.util.UUID

final case class Person(id: Long, identifier: UUID, name: String, birthDate: Instant)
