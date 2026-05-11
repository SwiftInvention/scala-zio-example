package com.example.notification.impl.service.repo.sql

import com.example.common.impl.repo.sql.SqlContext
import com.example.notification.impl.service.repo.sql.entity.NotificationPE

/** Quill schema declaration for the `notification` table. Mixed into the repo impl so `notificationTable` is in scope
  * alongside the Quill DSL. See the `pe-layout` principle.
  */
trait NotificationDbSchema {
  val ctx: SqlContext
  import ctx._

  protected val notificationTable = quote(querySchema[NotificationPE]("notification"))
}
