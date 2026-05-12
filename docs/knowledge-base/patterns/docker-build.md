# Docker Build

The `appServer` deployment unit is packaged as a docker image via sbt-native-packager's `DockerPlugin`. One image, env-driven config — same artifact runs in compose-local, dev, or prod, only the env vars differ.

## Build

```sh
just docker-build       # = sbt appServer/Docker/publishLocal
```

Tags the local docker daemon with `scala-zio-example-server:<version>` and `:latest`. Single-arch (host arch); multi-arch upgrade path at the bottom of this doc.

`sbt appServer/Docker/stage` produces `modules/app/server/target/docker/stage/Dockerfile` if you want to inspect what's generated without building.

## Image shape

Generated Dockerfile is multi-stage:

- **Builder stage** (`eclipse-temurin:21-jre-noble`) — COPYs the staged layout, applies `chmod` for runtime.
- **Runtime stage** (same base) — creates non-root user `app` (uid 1001), COPYs from the builder stage, sets the entrypoint, `EXPOSE 8080`.

The runtime layout has two cache layers — sbt-native-packager separates dependency JARs (rare change) from application JARs (frequent change), so changing app code keeps the deps layer cached.

OCI labels (`org.opencontainers.image.title`, `.description`, `.source`) are set in `build.sbt`'s `dockerSettings`.

## Config inside the image

The image carries all checked-in env files (`application-dev.conf`, `application-prod.conf`) plus, if the build host has it, the gitignored `application-local.conf`. Only the one selected by `APP_ENV` is loaded at boot; `local` is the on-host shape.

The dev/prod files use `${VAR}` env-var substitution for endpoints and credentials. Compose-local supplies those vars in `docker-compose.yml`'s `environment:` block — same shape as a real dev deployment, different values.

```yaml
services:
  server:
    image: scala-zio-example-server:latest
    environment:
      APP_ENV: dev
      MYSQL_URL: "jdbc:mysql://mysql:3306/localDatabase?..."
      MYSQL_USER: "localUser"
      MYSQL_PASSWORD: "localPassword"
      OTEL_ENDPOINT: "http://jaeger:4318/v1/traces"
```

A missing `${VAR}` fails the typesafe-config parse at boot.

## Run locally

```sh
just docker-run                    # bring up the server alongside infra
just docker-stop                   # stop just the server
just start-fresh-docker-server     # local-infra-reset → db-migrate → seed-example → docker-build → docker-run
```

Compose wires the server next to `mysql` and `jaeger` on the `scala-zio-example-local` network. The healthcheck hits `http://localhost:8080/health` via `curl` — once it passes, `docker compose up --wait` returns.

Hit it the same way as a host-run server:

```sh
just smoke-test    # curls the server on :8080
```

## Migrations stay out-of-process

The image is just the app — no Flyway, no migration runner. Schema changes apply via `just db-migrate` (or a separate Flyway container, or a CI step) before the server image is scaled up. This keeps "deliver code" and "change schema" as separate deployment actions: a code rollback doesn't undo a schema change.

## Multi-arch follow-up

Local builds are single-arch (host arch). For shipping to a registry that serves both amd64 and arm64 (M-series Macs, AWS Graviton), use `docker buildx`:

```sh
sbt appServer/Docker/stage
cd modules/app/server/target/docker/stage
docker buildx build --platform linux/amd64,linux/arm64 -t scala-zio-example-server:<tag> --push .
```

Multi-arch builds can't be loaded into the local docker image store without the containerd image store enabled; they only make sense paired with a registry push.
