package com.example.lib.common.domain.error.domain

import scala.collection.immutable

import com.example.lib.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait DomainErrorReason extends EnumEntry with ErrorReason

object DomainErrorReason extends Enum[DomainErrorReason] {
  val values: immutable.IndexedSeq[DomainErrorReason] = findValues

  case object InvalidEmail        extends DomainErrorReason
  case object InvalidCustomerName extends DomainErrorReason
  case object InvalidURL          extends DomainErrorReason
}
