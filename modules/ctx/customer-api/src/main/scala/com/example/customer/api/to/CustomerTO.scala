package com.example.customer.api.to

import com.example.common.domain.model.NewTypes.CustomerId
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class CustomerTO(
    id: CustomerId,
    email: String,
    name: String
)

object CustomerTO {
  implicit val codec: JsonCodec[CustomerTO] = DeriveJsonCodec.gen[CustomerTO]
}
