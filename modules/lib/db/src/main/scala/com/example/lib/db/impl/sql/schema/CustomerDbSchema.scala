package com.example.lib.db.impl.sql.schema

import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.impl.sql.entity.CustomerPE

/** Quill schema declaration for the `customer` table. */
trait CustomerDbSchema {
  val ctx: SqlContext
  import ctx._

  val customerTable = quote(querySchema[CustomerPE]("customer"))
}

object CustomerDbSchema {
  def apply(c: SqlContext): CustomerDbSchema = new CustomerDbSchema { val ctx: SqlContext = c }
}
