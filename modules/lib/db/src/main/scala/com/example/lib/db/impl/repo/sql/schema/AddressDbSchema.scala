package com.example.lib.db.impl.repo.sql.schema

import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.impl.repo.sql.entity.AddressPE

/** Quill schema declaration for the `address` table. */
trait AddressDbSchema {
  val ctx: SqlContext
  import ctx._

  val addressTable = quote(querySchema[AddressPE]("address"))
}

object AddressDbSchema {
  def apply(c: SqlContext): AddressDbSchema = new AddressDbSchema { val ctx: SqlContext = c }
}
