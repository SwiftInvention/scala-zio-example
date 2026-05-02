package com.example.common.domain.error.backend

import com.example.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait BackendErrorReason extends EnumEntry with ErrorReason

object BackendErrorReason extends Enum[BackendErrorReason] {
  val values: immutable.IndexedSeq[BackendErrorReason] = findValues

  case object InternalError extends BackendErrorReason
  case object Database      extends BackendErrorReason
}
