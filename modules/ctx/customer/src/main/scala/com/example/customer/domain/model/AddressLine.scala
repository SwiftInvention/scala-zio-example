package com.example.customer.domain.model

import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.error.InvalidAddressLineError
import zio._

/** Validated street address line. Trimmed; non-empty; bounded to 255 chars (matches DB column). */
sealed abstract case class AddressLine private (value: String)

object AddressLine {
  val MinLength = 1
  val MaxLength = 255

  def apply(s: String): AppIO[AddressLine] = {
    val normalized = s.trim
    val len        = normalized.length
    if (len >= MinLength && len <= MaxLength)
      ZIO.succeed(new AddressLine(normalized) {})
    else if (len < MinLength)
      ZIO.fail(InvalidAddressLineError(message = "Address line must not be empty"))
    else
      ZIO.fail(
        InvalidAddressLineError(message = s"Address line length $len exceeds max $MaxLength")
      )
  }
}
