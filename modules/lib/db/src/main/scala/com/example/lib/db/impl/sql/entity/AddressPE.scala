package com.example.lib.db.impl.sql.entity

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}

/** Persistence entity for the `address` table. */
final case class AddressPE(
    id: AddressId,
    customerId: CustomerId,
    line: String,
    city: String,
    postalCode: String
)
