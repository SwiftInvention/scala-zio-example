package com.example.customer.domain.model

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}

final case class Address(
    id: AddressId,
    customerId: CustomerId,
    line: AddressLine,
    city: City,
    postalCode: PostalCode
)
