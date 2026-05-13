package com.example.lib.db.impl.repo.sql.schema

import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.impl.repo.sql.entity.CustomerPE

/** Quill schema declaration for the `customer` table. Mixed into the repo impl so `customerTable` is in scope alongside
  * the Quill DSL. Other write paths (seed actions, test fixtures) construct an instance via the companion's `apply`.
  */
trait CustomerDbSchema {
  val ctx: SqlContext
  import ctx._

  // Table name `customer` is derived by SnakeCase from the type name `CustomerPE`.
  // Override here if the on-disk name diverges:
  //   schemaMeta[CustomerPE]("customers")
  val customerTable = quote(querySchema[CustomerPE]("customer"))
}

object CustomerDbSchema {
  def apply(c: SqlContext): CustomerDbSchema = new CustomerDbSchema { val ctx: SqlContext = c }
}
