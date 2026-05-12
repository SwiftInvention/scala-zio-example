# Commands

Commands an agent reaches for while iterating.

## Iteration loop

Three commands in iteration-cost order — pick the right one for the moment:

```sh
just compile         # tight loop: compile main + test, warnings as errors
just style-fix       # chunk closer: auto-fix formatting + scalafix; rewrites files
just precommit-fix   # done-gate: style-fix + style-check + unit tests + integration tests
```

`compile` is the fast feedback loop while writing code — reach for it after each edit while you're working through compile errors or shaping a function. `style-fix` runs scalafmt and scalafix; reach for it after a logical chunk of work, not between every keystroke (the file rewrites surprise you mid-edit). `precommit-fix` is the expected gate before declaring a code-touching task complete — it formats, lints, and runs the full test suite (including IT against test MySQL, which it brings up itself).

`style-check` exists for verify-only without rewrites:

```sh
just style-check     # verify formatting + lint, fail on issues; no rewrites
```

## Targeted tests

Per-spec sbt invocations when iterating on a specific spec:

```sh
sbt "ctxCustomer/testOnly *SpecName"
sbt "ctxCustomer/testOnly *SpecName -- -t \"test name\""
sbt "it/testOnly *SpecName"                              # integration specs
TEST_LOG_LEVEL=debug sbt "it/testOnly *SpecName"         # see logs while debugging
```

Integration specs are silent by default; `TEST_LOG_LEVEL=trace|debug|info|warn|error|fatal` enables a pretty console logger at the requested level. The env var inherits into sbt's forked test JVM. See [`patterns/logging.md`](patterns/logging.md#test-runs).

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
just run             # foreground server (Ctrl+C to stop)
```

`APP_ENV` comes from `.env` (seeded by `initial-setup` from `.env.example` — `APP_ENV=local`), so the server loads `application-local.conf`. Override by editing `.env` if you want a different env shape on the host.

`application-local.conf` points the OTLP exporter at the local Jaeger (`http://localhost:4318/v1/traces`). `AppTracing` probes the endpoint at boot; if Jaeger isn't reachable inside the probe budget the server fails at boot. Either run `just local-infra-up` first, or set `otel.otlp-endpoint = null` in your local conf to disable tracing. See [`patterns/tracing.md`](patterns/tracing.md) for what the probe checks.

## Dev tools (`appDev`)

```sh
just experiment      # run modules/app/dev/.../Experiment.scala against local MySQL
just seed-example    # run SeedExample — inserts Ada/Alan/Grace into customer + a few rows into notification
```

Both pick up `APP_ENV` from `.env` (`local` on host by default). `appDev` is local-only by build (see [`patterns/dev-tools.md`](patterns/dev-tools.md)) — no `APP_ENV=dev|prod` variants exist for it.

## Docker

```sh
just docker-build                  # build the server image via sbt-native-packager
just docker-run                    # bring up the dockerized server alongside infra
just docker-stop                   # stop just the dockerized server
just start-fresh-docker-server     # from any state → local-infra-reset → db-migrate → seed → docker-build → docker-run
```

Compose-local uses `APP_ENV=dev` and fills the `${VAR}` substitutions from `docker-compose.yml`'s `environment:` block. See [`patterns/docker-build.md`](patterns/docker-build.md).

## Setup

```sh
just initial-setup   # install JDK + sbt (SDKMAN), markdownlint-cli2 (npm, pinned); seed application-local.conf from .example
```

Every public recipe activates the pinned SDK env at the top, so it works from any shell without manual `sdk env` first.
