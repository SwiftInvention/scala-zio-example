# Module Layout

Three module types under `modules/`:

```text
modules/
├── lib/<name>/        shared infrastructure (no domain content)
├── ctx/<name>/        bounded context
├── ctx/<name>-api/    a context's cross-context API contract
└── app/<name>/        deployment unit (composition root + entrypoint)
```

sbt project IDs follow `<layer><CamelCaseName>`:

| Folder                          | sbt ID                |
| ------------------------------- | --------------------- |
| `modules/lib/common`            | `libCommon`           |
| `modules/ctx/customer`          | `ctxCustomer`         |
| `modules/ctx/customer-api`      | `ctxCustomerApi`      |
| `modules/app/server`            | `appServer`           |
| `modules/app/integration-tests` | `appIntegrationTests` |

## Lib internal structure

```text
modules/lib/<name>/src/main/scala/com/example/<name>/
├── domain/        traits, types, errors
└── impl/          concrete implementations
```

No `app/` — libs don't have an application surface. Same `domain` / `impl` split as a context.

`lib/common` example contents:

- `domain/error/AppFailure.scala` — base error class
- `domain/model/Types.scala` — effect aliases (`AppIO`, `AppRIO`)
- `domain/model/NewTypes.scala` — cross-cutting newtype IDs
- `domain/service/Transactor.scala` — transaction-boundary trait
- `impl/repo/sql/` — Quill context, datasource, encodings
- `impl/config/` — config bootstrap
- `impl/telemetry/AppTracing.scala` — OTLP exporter wiring
- `http/server/` — operational routes, wire-format `ApiFailure`, request middleware
- `http/client/` — outbound `Client` layer (`AppHttpClient`)

The `http/` subpackage sits next to `domain/` and `impl/` and holds HTTP transport adapters in both directions.

## App internal structure

Flat — no subfolders:

```text
modules/app/<name>/src/main/scala/com/example/app/<name>/
├── <Name>App.scala       entrypoint (extends ZIOAppDefault)
└── <Name>Env.scala       layer composition (AppEnv type alias + wiring)
```

`server/` is the canonical example. A future `migrator/` or `worker/` would follow the same shape.

`integration-tests/` is the test-only variant: it ships no `main` sources — just a `src/test/scala/com/example/app/integration/tests/` tree with `TestServer.scala` (the integration composition root) at the top and per-area sub-packages below it (`http/`, `customer/`, `notification/`, `common/`).

## When to add what

- **Shared utility used across contexts** → new `lib/`
- **New bounded context** → `ctx/<name>/` and `ctx/<name>-api/` together
- **New deployable unit** (worker, CLI, alternate API server) → new `app/<name>/`

For the ctx internal structure see [`bounded-context.md`](bounded-context.md). For how modules connect via `build.sbt`, see [`build-deps.md`](build-deps.md).
