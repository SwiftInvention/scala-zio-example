# Composition Roots

A composition root is the file that sees concrete implementations and wires them into a `ZLayer` graph for one entrypoint. Composition roots hold the global view of an entrypoint — the one place allowed to import across `bounded-context` / `import-direction` boundaries.

## The roots in this codebase

| Root                                          | Entrypoint           | What it wires                                                                  |
| --------------------------------------------- | -------------------- | ------------------------------------------------------------------------------ |
| `modules/app/server/.../ServerEnv.scala`      | `ServerApp`          | The production server: full layer stack + zio-http `Server`                    |
| `modules/app/integration-tests/.../TestServer.scala` | integration specs | The integration-test server: production stack with test substitutions       |
| `modules/app/dev/.../Experiment.scala`        | `Experiment`         | One-off local dev scratchpad — its own `provide(...)` block                    |
| `modules/app/dev/.../actions/*.scala`         | each action          | Each `ZIOAppDefault` action wires its own layers — no shared composition       |

A new `app/<name>/` module gets one.

## The rule: roots stay parallel

Different roots wire similar things in different ways (`DataSourceLayer` vs `TestDb.freshSchemaLayer`, `AppTracing.live` vs `AppTracing.liveWithoutProbe`, real `Server.Config` vs ephemeral). Don't factor that overlap into a shared layer set — the variation is what having more than one root buys you. Share *mechanisms* (a service trait + impl, a ctx) by lifting them into modules; let each root compose them itself.
