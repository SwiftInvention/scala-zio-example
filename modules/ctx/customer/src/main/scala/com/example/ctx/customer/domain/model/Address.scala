package com.example.ctx.customer.domain.model

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}

final case class Address(
    id: AddressId,
    customerId: CustomerId,
    line: AddressLine,
    city: City,
    postalCode: PostalCode
)
