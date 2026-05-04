# Domain

The example domain is one bounded context with one entity. The point is to show the patterns; the domain is intentionally skeletal.

## Customer

A customer of the system. Has an id, email, and display name.

| Term         | Meaning                                                                |
| ------------ | ---------------------------------------------------------------------- |
| `Customer`   | Domain entity (in `customer/domain/model/`)                            |
| `CustomerId` | Newtype-wrapped String id (in `lib/common/.../NewTypes`)               |
| `CustomerTO` | Wire format for the cross-context API and HTTP routes (in `customer-api/api/to/`) |

## Operations

Currently read-only.

- `find(id)` — Option-returning lookup. Used internally; not exposed via HTTP.
- `get(id)` — fail-on-missing lookup. Fails with `CustomerNotFoundError`. Backs `GET /customers/:id`.
- `list` — all customers. Backs `GET /customers`.

Cross-context API (`CustomerApi` in `customer-api`): TO-typed mirror of the same operations. No other context calls customer yet — the trait exists as scaffolding for when one does.
