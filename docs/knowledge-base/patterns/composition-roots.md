# Composition Roots

A composition root is the file that sees concrete implementations and wires them into a `ZLayer` graph for one entrypoint. Composition roots are where the global view of an entrypoint lives — they're the one place allowed to import across `bounded-context` / `import-direction` boundaries, because their job is to assemble those parts.

## The roots in this codebase

| Root                                          | Entrypoint           | What it wires                                                                  |
| --------------------------------------------- | -------------------- | ------------------------------------------------------------------------------ |
| `modules/app/server/.../ServerEnv.scala`      | `ServerApp`          | The production server: full layer stack + zio-http `Server`                    |
| `modules/app/it/.../TestServer.scala`         | integration specs    | The integration-test server: production stack with test substitutions          |
| `modules/app/dev/.../Experiment.scala`        | `Experiment`         | One-off local dev scratchpad — its own `provide(...)` block                    |
| `modules/app/dev/.../actions/*.scala`         | each action          | Each `ZIOAppDefault` action wires its own layers — no shared composition       |

## The rule: roots stay parallel

Different roots wire similar things in different ways. Don't factor the overlap into a shared layer set — the substantive variation across roots (`DataSourceLayer` vs `TestDb.freshSchemaLayer`, `AppTracing.live` vs `AppTracing.liveWithoutProbe`, real `Server.Config` vs ephemeral) is the point of having more than one root, and a shared piece becomes the place where one root's concerns leak into the others. Each root states its layer graph in full; readers see each entrypoint's answer side by side without chasing a factored helper.

## When to add a new composition root

A new `app/<name>/` module gets one. Three triggers:

- **A new deployment unit** (worker, batch job, alternate API server, migrator).
- **A scoped script that doesn't share production wiring** — `actions/` under `appDev`. Each one composes the slice it needs.
- **A test harness** that substitutes infrastructure. `TestServer` is the example in this codebase.

If the impulse is "we want to reuse some of the wiring," the right move is to lift the shared mechanism (a ctx, a service trait + impl), not the composition.
