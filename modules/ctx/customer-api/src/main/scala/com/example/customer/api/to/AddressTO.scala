package com.example.customer.api.to

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
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
