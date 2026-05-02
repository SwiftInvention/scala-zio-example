package com.example.customer.domain.error

import com.example.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable

sealed trait CustomerErrorReason extends EnumEntry with ErrorReason

object CustomerErrorReason extends Enum[CustomerErrorReason] {
  val values: immutable.IndexedSeq[CustomerErrorReason] = findValues

  case object NotFound extends CustomerErrorReason
}
