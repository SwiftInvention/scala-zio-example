package com.example.ctx.customer.domain.model

import com.example.ctx.customer.domain.error.InvalidCustomerNameError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated customer display name.
  *
  * Canonicalizes whitespace (trim) and enforces `1 <= length <= MaxLength`. The cap is a sanity bound, not a domain
  * rule — it exists to keep accidental paste-bombs out of the system. See the `smart-constructors` pattern doc.
  */
sealed abstract case class CustomerName private (value: String)

object CustomerName {
  val MinLength = 1
  val MaxLength = 200

  def apply(s: String): AppIO[CustomerName] = {
    val normalized = s.trim
    val len        = normalized.length
    if (len >= MinLength && len <= MaxLength)
      ZIO.succeed(new CustomerName(normalized) {})
    else if (len < MinLength)
      ZIO.fail(InvalidCustomerNameError(message = "Customer name must not be empty"))
    else
      ZIO.fail(
        InvalidCustomerNameError(message = s"Customer name length $len exceeds max $MaxLength")
      )
  }
}
