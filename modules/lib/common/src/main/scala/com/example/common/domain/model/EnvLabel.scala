package com.example.common.domain.model

import scala.collection.immutable

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}

/** Deployment environment tag.
  *
  * Parsed from the `APP_ENV` env var at boot. Selects which `application-<label>.conf` the app loads, and is also
  * available downstream (logs, metrics, feature gates).
  *
  * `entryName` is lowercase by convention — matches the conf-file suffix and the shell convention for env-var values.
  */
sealed trait EnvLabel extends EnumEntry with Lowercase

object EnvLabel extends Enum[EnvLabel] {
  val values: immutable.IndexedSeq[EnvLabel] = findValues

  case object Local extends EnvLabel
  case object Dev   extends EnvLabel
  case object Prod  extends EnvLabel
}
