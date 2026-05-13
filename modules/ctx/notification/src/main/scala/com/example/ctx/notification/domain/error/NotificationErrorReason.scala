package com.example.ctx.notification.domain.error

import scala.collection.immutable

import com.example.lib.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait NotificationErrorReason extends EnumEntry with ErrorReason

object NotificationErrorReason extends Enum[NotificationErrorReason] {
  val values: immutable.IndexedSeq[NotificationErrorReason] = findValues

  case object NotFound          extends NotificationErrorReason
  case object InvalidMessage    extends NotificationErrorReason
  case object InvalidChannel    extends NotificationErrorReason
  case object OrphanedRecipient extends NotificationErrorReason
}
