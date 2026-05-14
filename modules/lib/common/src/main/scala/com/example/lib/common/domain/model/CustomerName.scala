package com.example.lib.common.domain.model

import com.example.lib.common.domain.error.domain.InvalidCustomerNameError
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Validated customer display name. Trimmed; `1 <= length <= MaxLength`. Lives in `lib/common` because it crosses
  * bounded contexts — customer's `Customer` entity holds it, notification's recipient projection holds it as a
  * customer-derived view.
  */
sealed abstract case class CustomerName private (value: String)

object CustomerName {
  val MinLength = 1
  val MaxLength = 255

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
