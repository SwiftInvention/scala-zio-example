package com.example.lib.common.domain.error.backend

import scala.collection.immutable

import com.example.lib.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait BackendErrorReason extends EnumEntry with ErrorReason

object BackendErrorReason extends Enum[BackendErrorReason] {
  val values: immutable.IndexedSeq[BackendErrorReason] = findValues

  case object InternalError extends BackendErrorReason
  case object Database      extends BackendErrorReason
  case object Config        extends BackendErrorReason
  case object Timeout       extends BackendErrorReason
  case object DataIntegrity extends BackendErrorReason
}
