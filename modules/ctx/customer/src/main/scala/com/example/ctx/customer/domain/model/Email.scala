package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidEmailError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated email address.
  *
  * Smart constructor canonicalizes (trim + lowercase) and validates against a pragmatic regex. Construction goes
  * through `apply`, which is the only public path to a value. See [`smart-constructors.md`](smart-constructors.md) for
  * why the class declaration uses `sealed abstract case class ... private`.
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
