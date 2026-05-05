package com.example.common.domain.error.api

import scala.collection.immutable

import com.example.common.domain.error.ErrorReason
import enumeratum.{Enum, EnumEntry}

sealed trait ApiErrorReason extends EnumEntry with ErrorReason

object ApiErrorReason extends Enum[ApiErrorReason] {
  val values: immutable.IndexedSeq[ApiErrorReason] = findValues

  case object AuthenticationFailed    extends ApiErrorReason
  case object AuthorizationFailed     extends ApiErrorReason
  case object MalformedRequestContent extends ApiErrorReason
  case object MissingQueryParam       extends ApiErrorReason
  case object Validation              extends ApiErrorReason
  case object UrlPathNotFound         extends ApiErrorReason
  case object UnhandledError          extends ApiErrorReason
}
