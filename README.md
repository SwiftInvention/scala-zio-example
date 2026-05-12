# scala-zio-example

A reference template for Scala + ZIO monoliths organized by bounded contexts.

The patterns and architecture commitments live in [`docs/knowledge-base/`](docs/knowledge-base/) — start with [`overview.md`](docs/knowledge-base/overview.md). Day-to-day commands live in [`commands.md`](docs/knowledge-base/commands.md).

## Tech stack

- **Language / runtime**: Scala 2.13, JDK 21, sbt 1.12 (pinned in `.sdkmanrc`)
- **Effects**: ZIO 2.1
- **HTTP**: zio-http (typed `Endpoint` API, OpenAPI generation, Swagger UI at `/docs`)
- **Serialization**: zio-schema (single source of truth for TOs and newtypes; zio-json codec derived where needed)
- **Persistence**: Quill + MySQL; Flyway CLI for out-of-process migrations
- **Config**: PureConfig (typed, per-(app, env) HOCON files)
- **Telemetry**: zio-telemetry + OpenTelemetry SDK (OTLP HTTP exporter to local Jaeger by default)
- **Logging**: zio-logging + slf4j

Versions in [`project/Versions.scala`](project/Versions.scala); module wiring in [`build.sbt`](build.sbt).

## Prerequisites

- [`just`](https://github.com/casey/just) — task runner
- [SDKMAN](https://sdkman.io) — JDK + sbt are pinned in `.sdkmanrc`
- [Flyway](https://documentation.red-gate.com/fd/command-line-184127404.html) CLI — out-of-process migrations
- Docker — local MySQL + Jaeger via `docker compose`

## Quickstart

```sh
just initial-setup           # install JDK + sbt from .sdkmanrc; seed local config
just start-fresh-local-server # one command: infra reset → migrate → seed → run (foreground)
just smoke-test              # in another shell: hit the running server
```

Editor setup notes (Scalafmt, IntelliJ debugger): [`docs/intellij-idea-setup.md`](docs/intellij-idea-setup.md).

## Devcontainer

`.devcontainer/` runs the agent (Claude Code) in a network-isolated sandbox with inline `mysql` + `jaeger` services. Open the repo in VS Code and choose **Dev Containers: Open Folder in Container...**; sbt-driven recipes (`compile`, `test`, `run`, `db-migrate`, `style-*`) work inside. Pattern: [`patterns/devcontainer.md`](docs/knowledge-base/patterns/devcontainer.md). Base setup + per-host requirements: [SwiftInvention/devcontainers-example](https://github.com/SwiftInvention/devcontainers-example).
