# Overview

## What this is

A reference template for Scala + ZIO monoliths organized by bounded contexts. The example domain is intentionally thin — two bounded contexts where one calls the other through its `-api` contract — so the patterns are visible without domain noise. Specific patterns documented in [`patterns/`](patterns/); principles indexed in [`architecture-principles.md`](architecture-principles.md).

## Modules

| Folder                         | sbt ID               | Role                                                                                       |
| ------------------------------ | -------------------- | ------------------------------------------------------------------------------------------ |
| `modules/lib/common`           | `libCommon`          | shared infrastructure (effects, errors, IDs, config, telemetry, http server + client)      |
| `modules/lib/db`               | `libDb`              | shared persistence (schema, PEs, `SqlContext`, `Transactor`, migrations)                   |
| `modules/ctx/customer-api`     | `ctxCustomerApi`     | customer cross-context contract (trait + TOs) — consumed by notification                   |
| `modules/ctx/customer`         | `ctxCustomer`        | customer impl (domain, app, infra)                                                         |
| `modules/ctx/notification`     | `ctxNotification`    | notification impl; depends on `ctxCustomerApi` for recipient lookup. No `-api` module — nothing calls into notification |
| `modules/app/server`           | `appServer`          | deployment unit — composition root + entrypoint                                            |
| `modules/app/dev`              | `appDev`             | local-only dev tools — `Experiment` scratchpad + one-off `actions/`                        |
| `modules/app/integration-tests` | `appIntegrationTests` | test-only — integration specs against a real server + ephemeral MySQL schema             |

Module layout: [`patterns/module-layout.md`](patterns/module-layout.md). Cross-module deps: [`patterns/build-deps.md`](patterns/build-deps.md).

## System shape

A single zio-http server in `ServerApp` routes each request to the contributing ctx's `<Name>Routes`; routes call the ctx's app-service, which orchestrates service calls and any cross-context lookups via `<other-ctx>-api`. The composition root at `appServer/.../ServerEnv.scala` is the only place that sees concrete impls, wiring four tiers: config → infra → ctx → http. Migrations apply out-of-process via the `flyway` CLI.

## Tech stack

Scala 2.13 + ZIO 2.1 + zio-http + zio-schema + zio-prelude. Quill + MySQL for persistence; Flyway CLI for migrations. PureConfig for typed config. enumeratum for error enums. zio-telemetry + OpenTelemetry SDK for tracing. zio-logging + slf4j. JDK 21 + sbt 1.12, pinned via `.sdkmanrc`. Versions in `project/Versions.scala`, deps in `project/Dependencies.scala`.

## Everyday commands

- `just compile` — tight loop while writing code (compiles main + test, warnings as errors)
- `just style-fix` — scalafmt + scalafix; run after a logical chunk
- `just precommit-fix` — full gate before declaring a code-touching task done (style + unit + integration tests)
- `sbt "<moduleId>/testOnly *SpecName"` — targeted test run

Full reference (local server, docker, dev tools, setup): [`commands.md`](commands.md).
