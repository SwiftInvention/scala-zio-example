# Domain

The example domain is one bounded context (`customer`) with two entities. Intentionally skeletal — the point is to show the patterns without domain noise.

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

Cross-context API (`CustomerApi` in `customer-api`): TO-typed mirror of the same operations. No other context calls customer yet — the trait exists as scaffolding for when one does.
