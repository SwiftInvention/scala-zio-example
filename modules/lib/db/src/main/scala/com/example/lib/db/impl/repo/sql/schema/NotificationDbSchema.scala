package com.example.lib.db.impl.repo.sql.schema

import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.impl.repo.sql.entity.NotificationPE

/** Quill schema declaration for the `notification` table. Mixed into the repo impl so `notificationTable` is in scope
  * alongside the Quill DSL.
  */
trait NotificationDbSchema {
  val ctx: SqlContext
  import ctx._

  val notificationTable = quote(querySchema[NotificationPE]("notification"))
}

object NotificationDbSchema {
  def apply(c: SqlContext): NotificationDbSchema = new NotificationDbSchema { val ctx: SqlContext = c }
}
