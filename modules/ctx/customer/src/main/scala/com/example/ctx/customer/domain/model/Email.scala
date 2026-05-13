package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidEmailError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated email address. Trimmed, lowercased; validates against a pragmatic regex. */
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
