# Overview

## What this is

A reference template for Scala + ZIO monoliths organized by bounded contexts. Patterns are documented in [`patterns/`](patterns/) and summarized in [`architecture-principles.md`](architecture-principles.md).

The example domain is intentionally thin — one bounded context with a small entity set — so the patterns are visible without domain noise.

## Modules

| Folder                     | sbt ID           | Role                                                                |
| -------------------------- | ---------------- | ------------------------------------------------------------------- |
| `modules/lib/common`       | `libCommon`      | shared infrastructure (effects, errors, IDs, config, persistence)   |
| `modules/ctx/customer-api` | `ctxCustomerApi` | customer cross-context contract (trait + TOs)                       |
| `modules/ctx/customer`     | `ctxCustomer`    | customer impl (domain, app, infra)                                  |
| `modules/app/server`       | `appServer`      | deployment unit — composition root + entrypoint                     |
| `modules/app/dev`          | `appDev`         | local-only dev tools — `Experiment` scratchpad + one-off `actions/` |

Module layout: [`patterns/module-layout.md`](patterns/module-layout.md). Cross-module deps: [`patterns/build-deps.md`](patterns/build-deps.md).

## HTTP server

One zio-http server, started by `ServerApp` (`modules/app/server/.../ServerApp.scala`). Routes owned by contexts (`customer/impl/http/CustomerRoutes.scala`), composed at the app level. Host/port from `ServerConfig`.

Currently exposed:

- `GET /customers` — list (200, JSON array of `CustomerTO`)
- `GET /customers/:id` — fetch one (200, or 404 + `ErrorTO` body)
- `GET /customers/:id/addresses` — addresses owned by a customer (200, JSON array of `AddressTO`; empty array if the customer has none or doesn't exist)
- `GET /addresses/:id` — fetch one (200, or 404 + `ErrorTO` body)

## Service wiring

`ServerEnv.scala` is the composition root — the only place that sees concrete impls. Four tiers: config (`ConfigBootstrap` → typed `XConfig`s) → infra (datasource, transactor) → ctx (repo → service → app-service → api → routes) → http server. Adding a new ctx adds a leg to the third tier.

Config loaded from `application-${APP_ENV}.conf` at boot ([`patterns/config.md`](patterns/config.md)). Migrations apply out-of-process via `just db-migrate`. A service that exists but is never wired is dead code.

## Dev tools

`appDev` is a separate deployment unit for local-only one-off scripts: data seeding, scratchpad experiments, debugging actions. Each entrypoint (`Experiment.scala`, every action under `actions/`) is its own `ZIOAppDefault` with its own `provide(...)` block — no shared composition root. `SeedExampleCustomers` is the starter action; it seeds the example customers used by the smoke test. Local-only by build: `publish / skip := true` keeps the artifact off any deployment. Pattern: [`patterns/dev-tools.md`](patterns/dev-tools.md).

## Tech stack

Scala 2.13 + ZIO 2.1 + zio-http + zio-json + zio-prelude. Quill + MySQL for persistence; Flyway CLI for migrations. PureConfig for typed config. enumeratum for error enums. zio-logging + slf4j. JDK 21 + sbt 1.12, pinned via `.sdkmanrc`. Versions in `project/Versions.scala`, deps in `project/Dependencies.scala`.
