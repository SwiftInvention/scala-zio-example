package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidPostalCodeError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated postal code. Trimmed (preserving inner spaces), non-empty, bounded to 40 chars.
  *
  * Permissive on format because postal-code shapes vary wildly across countries. The codebase doesn't yet model country
  * to do per-country format validation; this constructor's job is to reject blank and overly-long values, not to assert
  * a specific format.
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
