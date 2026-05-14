package com.example.lib.db.impl.sql.schema

import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.impl.sql.entity.NotificationPE

/** Quill schema declaration for the `notification` table. */
trait NotificationDbSchema {
  val ctx: SqlContext
  import ctx._

  val notificationTable = quote(querySchema[NotificationPE]("notification"))
}

object NotificationDbSchema {
  def apply(c: SqlContext): NotificationDbSchema = new NotificationDbSchema { val ctx: SqlContext = c }
}
