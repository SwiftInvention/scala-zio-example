package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidPostalCodeError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated postal code. Trimmed (preserving inner spaces), non-empty, bounded to 40 chars. Format is intentionally
  * not validated — postal-code shapes vary by country and the domain has no country.
  */
sealed abstract case class PostalCode private (value: String)

object PostalCode {
  val MinLength = 1
  val MaxLength = 40

  def apply(s: String): AppIO[PostalCode] = {
    val normalized = s.trim
    val len        = normalized.length
    if (len >= MinLength && len <= MaxLength)
      ZIO.succeed(new PostalCode(normalized) {})
    else if (len < MinLength)
      ZIO.fail(InvalidPostalCodeError(message = "Postal code must not be empty"))
    else
      ZIO.fail(InvalidPostalCodeError(message = s"Postal code length $len exceeds max $MaxLength"))
  }
}
