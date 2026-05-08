# Commands

Commands an agent reaches for while iterating.

## Compile and test

```sh
just compile         # main + test, warnings as errors
just test            # run tests (no specs yet — recipe is in place)
```

Per-spec sbt invocations:

```sh
sbt "ctxCustomer/testOnly *SpecName"
sbt "ctxCustomer/testOnly *SpecName -- -t \"test name\""
sbt "it/testOnly *SpecName"                              # integration specs
TEST_LOG_LEVEL=debug sbt "it/testOnly *SpecName"         # see logs while debugging
```

Integration specs are silent by default; `TEST_LOG_LEVEL=trace|debug|info|warn|error|fatal` enables a pretty console logger at the requested level. The env var inherits into sbt's forked test JVM. See [`patterns/logging.md`](patterns/logging.md#test-runs).

## Formatting and linting

```sh
just style-fix       # auto-fix formatting + scalafix
just style-check     # verify only, fail on issues
```

## Running locally

```sh
just start-fresh-local-server   # one command, any starting state → working server foreground
just smoke-test                 # curl the running server; expects it up on :8080
```

`start-fresh-local-server` chains `local-infra-reset → db-migrate → seed-example → run` and blocks on the foreground server. Run `smoke-test` from a separate shell to hit it.

Individual pieces if you want finer-grained control:

```sh
just local-infra-up        # bring up MySQL + Jaeger (blocks until healthy)
just local-infra-down      # stop MySQL + Jaeger
just local-infra-reset     # wipe state and bring up fresh
just db-migrate      # apply Flyway migrations
just run             # foreground server (Ctrl+C to stop); sets APP_ENV=local
```

`run` and `start-fresh-local-server` export `APP_ENV=local` so the server loads `application-local.conf`. Other envs need `APP_ENV` set explicitly.

`application-local.conf` points the OTLP exporter at the local Jaeger (`http://localhost:4318/v1/traces`). If you run the server without Jaeger up, the exporter spams reconnect failures into the log; either run `just local-infra-up` first, or set `otel.otlp-endpoint = null` in your local conf to disable tracing.

## Dev tools (`appDev`)

```sh
just experiment      # run modules/app/dev/.../Experiment.scala against local MySQL
just seed-example    # run SeedExampleCustomers — inserts Ada/Alan/Grace into the customer table
```

Both export `APP_ENV=local`. `appDev` is local-only by build (see [`patterns/dev-tools.md`](patterns/dev-tools.md)) — no `APP_ENV=dev|prod` variants exist for it.

## Setup

```sh
just initial-setup   # install JDK + sbt from .sdkmanrc; seed application-local.conf from .example
```

Every public recipe activates the pinned SDK env at the top, so it works from any shell without manual `sdk env` first.
