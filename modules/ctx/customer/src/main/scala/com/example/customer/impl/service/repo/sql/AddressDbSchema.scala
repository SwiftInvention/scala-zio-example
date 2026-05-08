package com.example.customer.impl.service.repo.sql

import com.example.common.impl.repo.sql.SqlContext
import com.example.customer.impl.service.repo.sql.entity.AddressPE

/** Quill schema declaration for the `address` table. */
trait AddressDbSchema {
  val ctx: SqlContext
  import ctx._

  protected val addressTable = quote(querySchema[AddressPE]("address"))
}
