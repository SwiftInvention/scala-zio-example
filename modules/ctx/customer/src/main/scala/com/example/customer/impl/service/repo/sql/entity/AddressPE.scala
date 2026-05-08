package com.example.customer.impl.service.repo.sql.entity

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}

/** Persistence entity for the `address` table.
  *
  * Field names follow the Scala convention; `SnakeCase` naming on the Quill context maps to the DB columns
  * (`customer_id`, `postal_code`).
  */
final case class AddressPE(
    id: AddressId,
    customerId: CustomerId,
    line: String,
    city: String,
    postalCode: String
)
