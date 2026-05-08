package com.example.customer.impl.service.repo.sql

import com.example.common.impl.repo.sql.SqlContext
import com.example.customer.impl.service.repo.sql.entity.CustomerPE

/** Quill schema declaration for the `customer` table.
  *
  * Mixed into the repo impl so `customerTable` is in scope alongside the Quill DSL. Per-ctx by design — each ctx owns
  * the schema mapping for tables its repos query against. See the `pe-layout` principle.
  */
trait CustomerDbSchema {
  val ctx: SqlContext
  import ctx._

  // Table name `customer` is derived by SnakeCase from the type name `CustomerPE`.
  // Override here if the on-disk name diverges:
  //   schemaMeta[CustomerPE]("customers")
  protected val customerTable = quote(querySchema[CustomerPE]("customer"))
}
