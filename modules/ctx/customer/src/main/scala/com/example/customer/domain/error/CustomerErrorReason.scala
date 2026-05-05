package com.example.customer.domain.error

import scala.collection.immutable

import com.example.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait CustomerErrorReason extends EnumEntry with ErrorReason

object CustomerErrorReason extends Enum[CustomerErrorReason] {
  val values: immutable.IndexedSeq[CustomerErrorReason] = findValues

  case object NotFound     extends CustomerErrorReason
  case object InvalidEmail extends CustomerErrorReason
  case object InvalidName  extends CustomerErrorReason
}
