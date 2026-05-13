package com.example.lib.common.domain.model

import zio.prelude.Newtype
import zio.schema.Schema

/** Cross-cutting newtype definitions.
  *
  * Each id is a `Newtype[String]` — operations on the underlying string are deliberately hidden. Use `apply` to
  * construct, `unwrap` to escape.
  *
  * The implicit `Schema` on each newtype serializes to/from the underlying string (no wrapping object), so wire
  * payloads stay flat. zio-http's Endpoint API uses Schema directly; test infrastructure derives a zio-json codec from
  * Schema where needed (`zio.schema.codec.JsonCodec.jsonCodec(schema)`).
  */
object NewTypes {

  object CustomerId extends Newtype[String] {
    implicit val schema: Schema[Type] =
      Schema[String].transform(s => apply(s), t => unwrap(t))
  }
  type CustomerId = CustomerId.Type

  object AddressId extends Newtype[String] {
    implicit val schema: Schema[Type] =
      Schema[String].transform(s => apply(s), t => unwrap(t))
  }
  type AddressId = AddressId.Type

  object NotificationId extends Newtype[String] {
    implicit val schema: Schema[Type] =
      Schema[String].transform(s => apply(s), t => unwrap(t))
  }
  type NotificationId = NotificationId.Type
}
