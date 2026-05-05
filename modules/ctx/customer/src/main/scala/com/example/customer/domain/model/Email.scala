package com.example.customer.domain.model

import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.error.InvalidEmailError
import zio._

/** Validated email address.
  *
  * Smart constructor canonicalizes (trim + lowercase) and validates against a pragmatic regex. The triple `sealed
  * abstract case class ... private` is load-bearing — see the `smart-constructors` pattern doc for why each keyword is
  * required (in short: `abstract` suppresses case-class auto-`copy()`, which would otherwise leak validation).
  *
  * Construction goes through `apply`, which is the only public path to a value.
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
