# Commands

Commands an agent reaches for while iterating.

## Compile and test

```sh
just compile         # main + test, warnings as errors
just test            # run tests (no specs yet — recipe is in place)
```

Per-spec sbt invocations (when specs exist):

```sh
sbt "ctxCustomer/testOnly *SpecName"
sbt "ctxCustomer/testOnly *SpecName -- -t \"test name\""
```

## Formatting and linting

```sh
just style-fix       # auto-fix formatting + scalafix
just style-check     # verify only, fail on issues
```

## Running locally

```sh
just db-up           # start MySQL container (blocks until healthy)
just db-migrate      # apply Flyway migrations
just run             # foreground server (Ctrl+C to stop); sets APP_ENV=local
just smoke-test      # db-up + db-migrate + HTTP smoke (success path + typed-error path)
just db-reset        # wipe data volume and restart
```

The `run` and `smoke-test` recipes export `APP_ENV=local` so the server loads `application-local.conf`. Other envs need `APP_ENV` set explicitly.

## Setup

```sh
just initial-setup   # install JDK + sbt from .sdkmanrc; seed application-local.conf from .example
```

Every public recipe activates the pinned SDK env at the top, so it works from any shell without manual `sdk env` first.
