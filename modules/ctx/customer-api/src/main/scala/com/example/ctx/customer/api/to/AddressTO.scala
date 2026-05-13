package com.example.ctx.customer.api.to

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import zio.schema.{DeriveSchema, Schema}

final case class AddressTO(
    id: AddressId,
    customerId: CustomerId,
    line: String,
    city: String,
    postalCode: String
)

object AddressTO {
  implicit val schema: Schema[AddressTO] = DeriveSchema.gen[AddressTO]
}
