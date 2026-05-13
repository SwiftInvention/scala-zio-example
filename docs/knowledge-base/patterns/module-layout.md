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
- `impl/http/server/` — operational routes, request middleware
- `impl/http/client/` — outbound `Client` layer (`AppHttpClient`)
- `impl/http/{ApiFailure,ErrorTO}.scala` — wire-format error plumbing

HTTP server + client adapters live under `impl/http/`. The wire-format error types (`ApiFailure`, `ErrorTO`) sit at the top of `impl/http/`; `HttpError` (the status-code mixin on every `AppFailure`) lives in `domain/error/` because every typed error mixes it in at the type level.

## App internal structure

Apps are thin deployment units. Required at the module root:

```text
modules/app/<name>/src/main/scala/com/example/app/<name>/
├── <Name>App.scala       entrypoint (extends ZIOAppDefault)
└── <Name>Env.scala       layer composition (AppEnv type alias + wiring)
```

Beyond `App` + `Env`, an app organizes its own files however its deployment shape needs. `server/` adds `ServerRoutes.scala` (route composition feeding OpenAPI) and `config/ServerConfig.scala`. A worker might add a queue handler; a migrator might need nothing else.

`integration-tests/` is the test-only variant: no `main` sources, just `src/test/scala/com/example/app/integration/tests/` with `TestServer.scala` (the integration composition root) at the top and per-area sub-packages below it (`http/`, `customer/`, `notification/`, `common/`).

## When to add what

- **Shared utility used across contexts** → new `lib/`
- **New bounded context** → `ctx/<name>/` and `ctx/<name>-api/` together
- **New deployable unit** (worker, CLI, alternate API server) → new `app/<name>/`

For the ctx internal structure see [`bounded-context.md`](bounded-context.md). For how modules connect via `build.sbt`, see [`build-deps.md`](build-deps.md).
