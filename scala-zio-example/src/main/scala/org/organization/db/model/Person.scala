package org.organization.db.model

import java.time.Instant

final case class Person(id: Long, name: String, birthDate: Instant)
