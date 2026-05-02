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
just run             # foreground server (Ctrl+C to stop)
just smoke-test      # spin up server in background, exercise GET /customers and /customers/:id, tear down
```

`smoke-test` covers both the success path (200) and the typed-error path (404 with `ErrorTO` body).

## SDK setup

```sh
just initial-setup   # install JDK + sbt from .sdkmanrc via SDKMAN
```

Every public recipe activates the pinned SDK env at the top, so it works from any shell without manual `sdk env` first.
