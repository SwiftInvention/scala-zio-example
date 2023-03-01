package org.organization.db.model

import enumeratum.EnumEntry.Snakecase
import enumeratum.values._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
import io.circe.{Decoder, Encoder}

sealed abstract class Gender(val value: Int) extends IntEnumEntry with Snakecase
object Gender extends IntEnum[Gender] with IntQuillEnum[Gender] {

  val values: IndexedSeq[Gender] = findValues

  case object Male      extends Gender(0)
  case object Female    extends Gender(1)
  case object NonBinary extends Gender(2)

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")
  implicit val encoder: Encoder[Gender] = deriveConfiguredEncoder
  implicit val decoder: Decoder[Gender] = deriveConfiguredDecoder
}
