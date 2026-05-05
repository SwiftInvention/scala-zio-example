package com.example.common.domain.model

import zio.json.JsonCodec
import zio.prelude.Newtype

/** Cross-cutting newtype definitions.
  *
  * Each id is a `Newtype[String]` — operations on the underlying string are deliberately hidden. Use `apply` to
  * construct, `unwrap` to escape.
  *
  * The implicit `JsonCodec` on each newtype serializes to/from the underlying string (no wrapping object), so wire
  * payloads stay flat.
  */
object NewTypes {

  object CustomerId extends Newtype[String] {
    implicit val codec: JsonCodec[Type] =
      JsonCodec[String].transform(s => apply(s), t => unwrap(t))
  }
  type CustomerId = CustomerId.Type

  object AddressId extends Newtype[String] {
    implicit val codec: JsonCodec[Type] =
      JsonCodec[String].transform(s => apply(s), t => unwrap(t))
  }
  type AddressId = AddressId.Type
}
