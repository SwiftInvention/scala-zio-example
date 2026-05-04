package com.example.common.domain.error

import scala.collection.immutable

import enumeratum.{Enum, EnumEntry}

sealed trait ErrorCategory extends EnumEntry

object ErrorCategory extends Enum[ErrorCategory] {
  val values: immutable.IndexedSeq[ErrorCategory] = findValues

  case object Api      extends ErrorCategory
  case object Backend  extends ErrorCategory
  case object Customer extends ErrorCategory
}
