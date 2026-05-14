package com.example.lib.common.domain.model

import com.example.lib.common.domain.error.domain.InvalidEmailError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated email address. Trimmed, lowercased; validates against a pragmatic regex. Lives in `lib/common` because it
  * crosses bounded contexts — customer's `Customer` entity holds it, notification's recipient projection holds it as a
  * customer-derived view.
  */
sealed abstract case class Email private (value: String)

object Email {
  // Pragmatic — accepts the common shapes, rejects the obviously-malformed.
  // Not RFC 5321 perfect; that's not the goal.
  private val emailRegex = """^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""".r

  def apply(s: String): AppIO[Email] = {
    val normalized = s.trim.toLowerCase
    if (emailRegex.matches(normalized))
      ZIO.succeed(new Email(normalized) {})
    else
      ZIO.fail(InvalidEmailError(message = s"Invalid email: '$s'"))
  }
}
