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
  sbt "scalaZioExample/runMain org.organization.utils.DemoDb"
  ```

- Run

  ```sh
  sbt scalaZioExample/run
  ```

- Run in live reload mode

  ```sh
  sbt ~scalaZioExample/reStart
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

### Additional resources

[Setting up debugger in Intellij IDEA](/docs/intellij-idea-setup.md)