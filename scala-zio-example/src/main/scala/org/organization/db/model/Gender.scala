package org.organization.db.model

import enumeratum.EnumEntry.Snakecase
import enumeratum.values._

sealed abstract class Gender(val value: Int) extends IntEnumEntry with Snakecase
object Gender extends IntEnum[Gender] with IntQuillEnum[Gender] {

  val values = findValues

  case object Male      extends Gender(0)
  case object Female    extends Gender(1)
  case object NonBinary extends Gender(2)
}
