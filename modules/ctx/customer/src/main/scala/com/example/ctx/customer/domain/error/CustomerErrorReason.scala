package com.example.ctx.customer.domain.error

import scala.collection.immutable

import com.example.lib.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait CustomerErrorReason extends EnumEntry with ErrorReason

object CustomerErrorReason extends Enum[CustomerErrorReason] {
  val values: immutable.IndexedSeq[CustomerErrorReason] = findValues

  case object NotFound           extends CustomerErrorReason
  case object AddressNotFound    extends CustomerErrorReason
  case object InvalidAddressLine extends CustomerErrorReason
  case object InvalidCity        extends CustomerErrorReason
  case object InvalidPostalCode  extends CustomerErrorReason
}
