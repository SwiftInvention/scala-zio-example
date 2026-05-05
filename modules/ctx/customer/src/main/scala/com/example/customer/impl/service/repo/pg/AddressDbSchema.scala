package com.example.customer.impl.service.repo.pg

import com.example.common.impl.repo.pg.PgContext
import com.example.customer.impl.service.repo.pg.entity.AddressPE

/** Quill schema declaration for the `address` table. */
trait AddressDbSchema {
  val ctx: PgContext
  import ctx._

  protected val addressTable = quote(querySchema[AddressPE]("address"))
}
