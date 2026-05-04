package com.example.common.impl.repo.pg

import com.example.common.domain.model.NewTypes.CustomerId
import io.getquill.MappedEncoding

/** Quill `MappedEncoding`s for cross-cutting newtypes.
  *
  * Mixed into `PgContext` so Quill can encode/decode `Newtype[String]` ids
  * transparently. As new ids are added in `NewTypes`, mirror them here.
  */
trait NewTypeEncodings {

  implicit val customerIdEncoding: MappedEncoding[CustomerId, String] =
    MappedEncoding[CustomerId, String](id => CustomerId.unwrap(id))
  implicit val customerIdDecoding: MappedEncoding[String, CustomerId] =
    MappedEncoding[String, CustomerId](s => CustomerId(s))
}
