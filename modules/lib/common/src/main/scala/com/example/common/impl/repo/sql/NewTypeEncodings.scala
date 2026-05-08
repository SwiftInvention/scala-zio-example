package com.example.common.impl.repo.sql

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import io.getquill.MappedEncoding

/** Quill `MappedEncoding`s for cross-cutting newtypes.
  *
  * Mixed into `SqlContext` so Quill can encode/decode `Newtype[String]` ids transparently. As new ids are added in
  * `NewTypes`, mirror them here.
  */
trait NewTypeEncodings {

  implicit val customerIdEncoding: MappedEncoding[CustomerId, String] =
    MappedEncoding[CustomerId, String](id => CustomerId.unwrap(id))
  implicit val customerIdDecoding: MappedEncoding[String, CustomerId] =
    MappedEncoding[String, CustomerId](s => CustomerId(s))

  implicit val addressIdEncoding: MappedEncoding[AddressId, String] =
    MappedEncoding[AddressId, String](id => AddressId.unwrap(id))
  implicit val addressIdDecoding: MappedEncoding[String, AddressId] =
    MappedEncoding[String, AddressId](s => AddressId(s))
}
