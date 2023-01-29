package org.organization.api.model

import java.util.UUID

import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps

object NewType {

  @newtype case class PersonIdentifier(value: UUID)

  object PersonIdentifier {
    def fromUUID(id: UUID): PersonIdentifier = id.coerce

    implicit val decoder: Decoder[PersonIdentifier] = Decoder.decodeUUID.map(fromUUID)
    implicit val encoder: Encoder[PersonIdentifier] = Encoder.encodeUUID.contramap(_.value)
  }
}
