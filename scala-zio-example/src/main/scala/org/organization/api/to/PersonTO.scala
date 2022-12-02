package org.organization.api.to

import java.time.Instant
import java.util.UUID

final case class PersonTO(identifier: UUID, name: String, birthDate: Instant)
