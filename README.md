# ScalaZioExample project

## Prerequisites

- `sbt`
- Docker
- docker-compose `v2` or higher (Note for macOS: enable `Use Docker Compose V2` in Docker Preferences)
- OpenJDK 11 or above

## Setting up

### Scalafmt Editor support

- [VS Code][vscode]
- [Intellij IDEA][intellij]

[vscode]: https://scalameta.org/metals/docs/editors/vscode/
[intellij]: https://scalameta.org/scalafmt/docs/installation.html#intellij

## Usage

- Start MySQL in docker container

  Note: flags and options mean:

  - use detached mode
  - wait for all containers to be healthy
  - remove volumes on exit

  ```sh
  docker compose up -Vd --wait mysql
  ```

- Fill db by demo data

  ```sh
  sbt dev "scalaZioExample/runMain org.organization.utils.DemoDb"
  ```

- Run

  ```sh
  sbt dev scalaZioExample/run
  ```

- Run in live reload mode

  ```sh
  sbt dev ~scalaZioExample/reStart
  ```

- When done, remove docker containers, networks and volumes

  ```sh
  docker compose down -v
  ```

- Clean build artifacts and recompile (just in case)

  ```sh
  sbt clean compile
  ```

- Run tests

  ```sh
  sbt test
  ```

- Check for dependency updates (just in case)

  ```sh
    sbt dependencyUpdates
  ```

### Swagger-UI

Go to `http://localhost:8080/docs`

### Scalafix

- `sbt scalafix` – run linter, check all files, fail on warnings

### Tpolecat

There are several modes: `dev`, `ci`, `release`. In `dev` mode "fatal warnings" are disabled, so you will be able to see warnings, but they won't prevent code from compiling.

- You can change tpolecat plugin mode in sbt interactively, e.g: `sbt ci` or `sbt dev`
- You can prepend your commands with mode, e.g: `sbt ci test` or `sbt dev test`
- You can set mode via env variable, e.g: `SBT_TPOLECAT_DEV=true sbt compile`, `SBT_TPOLECAT_CI=true sbt compile`, or `SBT_TPOLECAT_RELEASE=true sbt compile`

### Migrating to ZIO2

- Apply the `Zio2Upgrade` scalafix rule, as described in
  the [migration guide](https://zio.dev/guides/migrate/zio-2.x-migration-guide/#automatic-migration)
- Update dependencies
  - Bump versions on core zio dependencies (`zio`, `zio-streams`, `zio-test`)
  - Remove pins on dependencies which were pinned to prevent a transitive dependency on ZIO2
  - For other ZIO libraries, check their respective docs for a way to correctly add a version with ZIO2 support.
    Some require you to specify a different organization name or artefact ID:
    - `"io.d11" % "zhttp_2.13"` -> `"dev.zio" %% "zio-http"`
    - `"com.softwaremill.sttp.tapir" %% "tapir-zio1-http-server"` -> `"com.softwaremill.sttp.tapir" %% "tapir-zio-http-server"`
    - `"io.github.scottweaver" %% "zio-testcontainers-mysql"` -> `"io.github.scottweaver" %% "zio-2-0-testcontainers-mysql"`
- Fix the remaining compilation errors referring
  to the [migration guide](https://zio.dev/guides/migrate/zio-2.x-migration-guide).
- See [PR with all the changes listed above](https://github.com/SwiftInvention/scala-zio-example/pull/38)

### Additional resources

[Setting up debugger in Intellij IDEA](/docs/intellij-idea-setup.md)
