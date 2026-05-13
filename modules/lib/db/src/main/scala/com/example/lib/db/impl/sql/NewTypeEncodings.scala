package com.example.lib.db.impl.sql

import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId, NotificationId}
import io.getquill.MappedEncoding

/** Quill `MappedEncoding`s for cross-cutting newtypes. Mixed into `SqlContext` so Quill encodes/decodes
  * `Newtype[String]` ids transparently.
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

  implicit val notificationIdEncoding: MappedEncoding[NotificationId, String] =
    MappedEncoding[NotificationId, String](id => NotificationId.unwrap(id))
  implicit val notificationIdDecoding: MappedEncoding[String, NotificationId] =
    MappedEncoding[String, NotificationId](s => NotificationId(s))
}
