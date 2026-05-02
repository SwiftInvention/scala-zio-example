package com.example.common.domain.error.api

import com.example.common.domain.error.AppFailure
import zio.json.{DeriveJsonCodec, JsonCodec}

/** Wire format for any `AppFailure` rendered at the HTTP boundary. */
final case class ErrorTO(code: Int, category: String, reason: String, description: String)

object ErrorTO {
  implicit val codec: JsonCodec[ErrorTO] = DeriveJsonCodec.gen[ErrorTO]

  def from(e: AppFailure): ErrorTO =
    ErrorTO(
      code = e.responseCode,
      category = e.category.entryName,
      reason = e.reason.toString,
      description = e.description
    )
}
