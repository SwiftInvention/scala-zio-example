package com.example.lib.db.impl.sql.entity

import com.example.lib.common.domain.model.NewTypes.CustomerId

/** Persistence entity for the `customer` table. */
final case class CustomerPE(
    id: CustomerId,
    email: String,
    name: String
)
