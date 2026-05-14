package com.example.lib.common.domain.model

import scala.collection.immutable

import enumeratum.EnumEntry.Lowercase
import enumeratum.{Enum, EnumEntry}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

/** Selects which console output format the runtime logger uses.
  *
  *   - `Pretty` — colored, multi-line-friendly. Used in local/dev for human tailing.
  *   - `Json` — single-line JSON. Used in deployed envs for log aggregators.
  *
  * Set per-(app, env) under `logging.format`. No default in code per the `config-shape` principle.
  */
sealed trait LogFormatKind extends EnumEntry with Lowercase

object LogFormatKind extends Enum[LogFormatKind] {
  val values: immutable.IndexedSeq[LogFormatKind] = findValues

  case object Pretty extends LogFormatKind
  case object Json   extends LogFormatKind

  /** PureConfig reader: matches case-insensitively against the lowercase `entryName` (`pretty` / `json`). */
  implicit val configReader: ConfigReader[LogFormatKind] =
    ConfigReader.fromString { s =>
      withNameInsensitiveOption(s).toRight(
        CannotConvert(s, "LogFormatKind", s"valid: ${values.map(_.entryName).mkString("|")}")
      )
    }
}
