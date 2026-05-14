package com.example.lib.common.impl.http

import com.example.lib.common.domain.error.AppFailure
import zio.schema.{DeriveSchema, Schema}

/** Wire format for any `AppFailure` rendered at the HTTP boundary. */
final case class ErrorTO(code: Int, category: String, reason: String, description: String)

object ErrorTO {
  implicit val schema: Schema[ErrorTO] = DeriveSchema.gen[ErrorTO]

  def from(e: AppFailure): ErrorTO =
    ErrorTO(
      code = e.responseCode,
      category = e.category.entryName,
      reason = e.reason.toString,
      description = e.description
    )
}
