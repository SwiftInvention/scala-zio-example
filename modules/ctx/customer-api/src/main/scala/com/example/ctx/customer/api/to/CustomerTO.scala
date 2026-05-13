package com.example.ctx.customer.api.to

import com.example.lib.common.domain.model.NewTypes.CustomerId
import zio.schema.{DeriveSchema, Schema}

final case class CustomerTO(
    id: CustomerId,
    email: String,
    name: String
)

object CustomerTO {
  implicit val schema: Schema[CustomerTO] = DeriveSchema.gen[CustomerTO]
}
