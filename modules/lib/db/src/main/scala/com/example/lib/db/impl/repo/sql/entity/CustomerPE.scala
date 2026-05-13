package com.example.lib.db.impl.repo.sql.entity

import com.example.lib.common.domain.model.NewTypes.CustomerId

/** Persistence entity for the `customer` table. One PE per table, mirroring its columns; consumed by repo impls in any
  * ctx that queries the table, with `CustomerPEConverter` (in `ctx/customer`) bridging to the domain `Customer`.
  *
  * Field names follow the Scala convention; `SnakeCase` naming strategy on the Quill context handles the column-name
  * mapping at query time.
  */
final case class CustomerPE(
    id: CustomerId,
    email: String,
    name: String
)
