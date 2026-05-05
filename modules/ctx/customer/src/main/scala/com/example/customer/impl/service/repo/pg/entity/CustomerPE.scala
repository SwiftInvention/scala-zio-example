package com.example.customer.impl.service.repo.pg.entity

import com.example.common.domain.model.NewTypes.CustomerId

/** Persistence entity for the `customer` table.
  *
  * One PE per table, mirroring its columns. PEs don't leave `impl/` — repo methods convert to/from `Customer` via
  * `CustomerPEConverter` before returning. See the `pe-layout` and `pe-converters` principles.
  *
  * Field names follow the Scala convention; `SnakeCase` naming strategy on the Quill context handles the column-name
  * mapping at query time.
  */
final case class CustomerPE(
    id: CustomerId,
    email: String,
    name: String
)
