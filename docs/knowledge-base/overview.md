# Overview

## What this is

A reference template for Scala + ZIO monoliths organized by bounded contexts. The patterns here (module layout, ctx structure, error model, etc.) are documented in [`patterns/`](patterns/) and summarized in [`architecture-principles.md`](architecture-principles.md).

The example domain is intentionally thin (one context, one entity) so the patterns are visible without domain noise.

## Modules

| Folder                     | sbt ID           | Role                                            |
| -------------------------- | ---------------- | ----------------------------------------------- |
| `modules/lib/common`       | `libCommon`      | shared infrastructure (effects, errors, IDs)    |
| `modules/ctx/customer-api` | `ctxCustomerApi` | customer cross-context contract (trait + TOs)   |
| `modules/ctx/customer`     | `ctxCustomer`    | customer impl (domain, app, infra)              |
| `modules/app/server`       | `appServer`      | deployment unit — composition root + entrypoint |

Module-layout convention: [`patterns/module-layout.md`](patterns/module-layout.md). Cross-module deps: [`patterns/build-deps.md`](patterns/build-deps.md).

## HTTP server

One zio-http server, default port 8080, started by `ServerApp` (`modules/app/server/.../ServerApp.scala`). Routes are owned by contexts (`customer/impl/http/CustomerRoutes.scala`) and composed at the app level.

Currently exposed:

- `GET /customers` — list (200, JSON array of `CustomerTO`)
- `GET /customers/:id` — fetch one (200, or 404 + `ErrorTO` body via the typed-error path)

## Service wiring

`ServerEnv.scala` is the single place that sees concrete impls and wires them. Layer chain for the customer context:

```
CustomerRepoImpl.layer
  → CustomerServiceImpl.layer
    → CustomerAppServiceImpl.layer
      → CustomerApiDirectImpl.layer
        → CustomerRoutes.layer
+ Server.default
```

A service that exists but is never wired in `ServerEnv` is dead code.

## Tech stack

- Scala **2.13.18**, sbt **1.12.10**, JDK **Temurin 21** (pinned via `.sdkmanrc`)
- ZIO 2.1.x effect system
- zio-http for HTTP
- zio-json for JSON codecs
- zio-prelude `Newtype`s for typed IDs (`lib/common/.../NewTypes`)
- Quill + MySQL + Flyway for DB (deps wired, repos are stubs for now)
- enumeratum for error category/reason enums
- pureconfig for config (deps wired, no usage yet)
- zio-logging + slf4j bridges
