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
| `AddressTO` | Wire format (in `customer-api/api/to/`)                  |

`AddressLine`, `City`, and `PostalCode` are smart-constructor value objects in the same domain package.

## Operations

Currently read-only.

Customer:

- `find(id)` — Option-returning lookup. Used internally; not exposed via HTTP.
- `get(id)` — fail-on-missing lookup. Fails with `CustomerNotFoundError`. Backs `GET /customers/:id`.
- `list` — all customers. Backs `GET /customers`.

Address:

- `findAddress(id)` — Option-returning lookup. Used internally; not exposed via HTTP.
- `getAddress(id)` — fail-on-missing lookup. Fails with `AddressNotFoundError`. Backs `GET /addresses/:id`.
- `listAddressesForCustomer(customerId)` — addresses owned by a customer. Pass-through to the repo: returns an empty list if the customer has no addresses or doesn't exist (no existence check). Backs `GET /customers/:id/addresses`.

Cross-context API (`CustomerApi` in `customer-api`): TO-typed surface that other contexts call into. Holds `get` (existence check) and `getMany` (batch lookup for list-enrichment) — the operations the notification ctx needs.

## Notification

A notification record attached to a customer. Created via `POST /notifications`; can be listed, fetched by id, or filtered by recipient.

| Term                        | Meaning                                                                              |
| --------------------------- | ------------------------------------------------------------------------------------ |
| `Notification`              | Domain entity (in `notification/domain/model/`)                                      |
| `NotificationId`            | Newtype-wrapped String id (in `lib/common/.../NewTypes`)                             |
| `NotificationChannel`       | Sealed ADT: `Email` \| `Sms` \| `InApp`. Closed set; enumeratum `entryName` on the wire |
| `NotificationMessage`       | Smart-constructor value object (non-empty, length-capped)                            |
| `NotificationRecipient`     | Notification-domain view of a customer (id, email, name). Decoupled from `CustomerTO` |
| `NotificationWithRecipient` | Domain pair returned by enriched read paths                                          |
| `NotificationTO`            | Wire format for the notification itself                                              |
| `NotificationRecipientTO`   | Wire format for the recipient projection (self-contained — notification-api doesn't import customer-api) |

### Operations

- `create(recipientId, channel, message)` — creates a record. Calls `CustomerApi.get(recipientId)` for the existence check; `CustomerNotFoundError` propagates unchanged. Backs `POST /notifications`.
- `find(id)` — Option-returning lookup. Used internally; not exposed via HTTP.
- `get(id)` — fail-on-missing lookup; enriches with recipient via `CustomerApi.getMany`. Backs `GET /notifications/:id`.
- `list` — all notifications, each enriched with its recipient (batched via `CustomerApi.getMany`). Backs `GET /notifications`.
- `listForRecipient(recipientId)` — notifications scoped to one customer. No enrichment (recipient is implied by the path). Backs `GET /customers/:id/notifications`.

The two cross-context interactions — existence check on `create`, batch enrichment on `get` / `list` — both live in `NotificationAppService`, not in the route. See [`patterns/cross-context-call.md`](patterns/cross-context-call.md).
