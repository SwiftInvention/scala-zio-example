package com.example.customer.domain.model

import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.error.InvalidCityError
import zio._

/** Validated city name. Trimmed; non-empty; bounded to 120 chars (matches DB column). */
sealed abstract case class City private (value: String)

object City {
  val MinLength = 1
  val MaxLength = 120

  def apply(s: String): AppIO[City] = {
    val normalized = s.trim
    val len        = normalized.length
    if (len >= MinLength && len <= MaxLength)
      ZIO.succeed(new City(normalized) {})
    else if (len < MinLength)
      ZIO.fail(InvalidCityError(message = "City must not be empty"))
    else
      ZIO.fail(InvalidCityError(message = s"City length $len exceeds max $MaxLength"))
  }
}
