# Domain

The example domain is two bounded contexts: `customer` (entities + addresses) and `notification` (records attached to a customer, with a cross-context call to fetch the recipient). Intentionally skeletal — the point is to show the patterns without domain noise.

## Customer

A customer of the system. Has an id, email, and display name.

| Term         | Meaning                                                                           |
| ------------ | --------------------------------------------------------------------------------- |
| `Customer`   | Domain entity (in `customer/domain/model/`)                                       |
| `CustomerId` | Newtype-wrapped String id (in `lib/common/.../NewTypes`)                          |
| `CustomerTO` | Wire format for the cross-context API and HTTP routes (in `customer-api/api/to/`) |

`Email` and `CustomerName` are smart-constructor value objects in the same domain package — validation happens at construction.

## Address

A postal address belonging to a customer. Has an id, an owning customer id, a street line, a city, and a postal code.

| Term        | Meaning                                                  |
| ----------- | -------------------------------------------------------- |
| `Address`   | Domain entity (in `customer/domain/model/`)              |
| `AddressId` | Newtype-wrapped String id (in `lib/common/.../NewTypes`) |
| `AddressTO` | Wire format (in `customer/impl/to/` — HTTP-only, not cross-context) |

`AddressLine`, `City`, and `PostalCode` are smart-constructor value objects in the same domain package.

## Notification

A notification record attached to a customer. The notification ctx depends on `customer-api` for recipient lookup.

| Term                        | Meaning                                                                              |
| --------------------------- | ------------------------------------------------------------------------------------ |
| `Notification`              | Domain entity (in `notification/domain/model/`)                                      |
| `NotificationId`            | Newtype-wrapped String id (in `lib/common/.../NewTypes`)                             |
| `NotificationChannel`       | Sealed ADT: `Email` \| `Sms` \| `InApp`. Closed set; enumeratum `entryName` on the wire |
| `NotificationMessage`       | Smart-constructor value object (non-empty, length-capped)                            |
| `NotificationRecipient`     | Notification-domain view of a customer (id, email, name). Decoupled from `CustomerTO` |
| `NotificationWithRecipient` | Domain pair returned by enriched read paths                                          |
| `NotificationTO`            | Wire format for the notification itself (in `notification/impl/to/`)                 |
| `NotificationRecipientTO`   | Wire format for the recipient projection — self-contained, decoupled from `CustomerTO` (in `notification/impl/to/`) |
