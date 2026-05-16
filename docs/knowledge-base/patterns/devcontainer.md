# Devcontainer

VS Code Dev Container that runs the agent (Claude Code) in a network-isolated sandbox. Outbound traffic flows through a WireGuard tunnel to a mitmproxy proxy that injects secrets only for allow-listed hosts.

The base configuration comes from [SwiftInvention/devcontainers-example](https://github.com/SwiftInvention/devcontainers-example) (the `scala/` stack).

## Layout

```text
.devcontainer/
  Dockerfile.app             # agent image: mise-managed toolchains + project tools
  compose-all.yml            # full stack: agent + mitmproxy + wg-client + mysql + jaeger
  devcontainer.json          # VS Code config (extensions, postStart, remoteEnv)
  sandcat/                   # WireGuard + mitmproxy + entrypoint scripts (from base)
.sandcat/
  settings.json              # project-specific network/secrets overrides
.env.example                 # host shape (just-recipe env vars); `initial-setup` copies → .env
.env.devcontainer            # devcontainer shape; consumed by compose-all.yml's `env_file`
```

## What's repo-specific

1. **`Dockerfile.app` — toolchain pins.** mise installs `java@temurin-21.0.11`, `scala@2.13.18`, `sbt@1.12.10`, `just@1.36.0`, `flyway@12.5.0`. The same toolchain versions are pinned in four other places on bump — see "Version pin sites" below.
2. **`compose-all.yml` — inline infra.** `mysql`, `mysql-test`, and `jaeger` services run alongside the agent in the same compose project. The agent shares wg-client's network namespace and reaches them by service name (`mysql:3306`, `mysql-test:3306`, `jaeger:4318`). The agent's `env_file: ../.env.devcontainer` pulls `APP_ENV=local` plus the two `${?VAR}` overrides (`MYSQL_URL`, `OTEL_ENDPOINT`) that `application-local.conf` picks up in place of its host-localhost defaults.
3. **Sandbox name.** `scala-zio-example-sandbox` appears in `devcontainer.json` (`name`, `workspaceFolder`, `postStartCommand`), `compose-all.yml` (`name`, volume paths, `working_dir`), and `sandcat/scripts/app-user-init.sh` (`safe.directory`). Unique per repo to avoid Docker Compose project-name collisions across devcontainers.

## Inside the agent

The agent has no docker socket — it can run sbt, just, flyway, and curl against the inline services, but can't orchestrate compose. Recipes split into three buckets:

**Work as-is:** `compile`, `test`, `test-it`, `style-check`, `style-fix`, `precommit-fix`, `run`, `experiment`, `seed-example`, `smoke-test`, `db-migrate`. These hit sbt or flyway, which talk to the inline `mysql` / `mysql-test` / `jaeger`. `test-it` detects the devcontainer via the `DEVCONTAINER` env marker and skips its host-only docker dance — `mysql-test` is already up and migrated (compose mounts the migration files into mysql's initdb directory, so they apply on container init). This shortcut requires migrations to be plain SQL only — flyway-specific syntax (Java migrations, callbacks, `${flyway:VAR}` placeholders, repeatable `R__*.sql`) won't run via initdb.

**Don't apply:** `local-infra-up`, `local-infra-down`, `local-infra-reset`, `test-infra-up`, `test-infra-down`, `test-infra-reset`, `test-db-migrate`. The devcontainer brings up its own infra when it starts.

**Don't work without docker access:** `docker-build`, `docker-run`, `docker-stop`. Run these from the host instead.

`just initial-setup` is also host-only — it installs SDKMAN (the agent uses mise) and seeds host `.env` + `application-local.conf` files. Inside the agent, none of that applies; `just compile` works directly.

## How env vars route between host and devcontainer

`.env` is loaded by `just` (`set dotenv-load := true` in the justfile), so every recipe inherits its values:

- **Host:** `initial-setup` copies `.env.example` → `.env`. `APP_ENV=local` (selects `application-local.conf`'s defaults — `localhost:3306` jdbc, `localhost:4318` otlp). `MYSQL_HOST=localhost` for `just db-migrate`'s flyway URL.
- **Devcontainer:** `compose-all.yml`'s agent reads `.env.devcontainer` directly via `env_file`. Same `APP_ENV=local` selects the same conf, but `MYSQL_URL` and `OTEL_ENDPOINT` are set — the `${?VAR}` overrides in `application-local.conf` swap in `mysql:3306` and `jaeger:4318`. `MYSQL_HOST=mysql` for flyway. The same env vars reach `just` recipes (which inherit the agent's process env).

`.env.devcontainer` is checked in — treat its contents as public.

## Version pin sites

Five files pin language/tool versions; bumping requires updating all of them:

| Tool       | `.sdkmanrc` | `project/build.properties` | `project/Versions.scala` | `.github/workflows/cicd.yml` | `.devcontainer/Dockerfile.app` |
| ---------- | ----------- | -------------------------- | ------------------------ | ---------------------------- | ------------------------------ |
| java       | ✓           |                            |                          |                              | ✓                              |
| sbt        | ✓           | ✓ (exact match required)   |                          |                              | ✓                              |
| scala      |             |                            | (via `build.sbt`)        |                              | ✓                              |
| just       |             |                            |                          | ✓ (`JUST_VERSION`)           | ✓                              |
| flyway CLI |             |                            | ✓                        | ✓ (`FLYWAY_VERSION`)         | ✓                              |

sbt's launcher reads `project/build.properties` at runtime, so the Dockerfile pin and the file must agree or the launcher silently downloads a different version.

## Setup

Per-host one-time setup is in the [SwiftInvention/devcontainers-example README](https://github.com/SwiftInvention/devcontainers-example#local-setup) — generate a GitHub PAT and a Claude Code OAuth token, write them into `~/.config/sandcat/settings.json` alongside the allow-list. After that, **Dev Containers: Open Folder in Container...** in VS Code on this repo brings up the full stack and drops you into the agent.

## What sandcat protects

- **Network isolation.** The agent's outbound traffic goes through wg-client's WireGuard tunnel, then through mitmproxy. Hosts not in `settings.json`'s `network` allow-list are rejected at the proxy.
- **Secret injection at the boundary.** Secrets like `GITHUB_TOKEN` are stored only in mitmproxy. The agent sees placeholder values; mitmproxy substitutes the real token in flight, but only for hosts the secret is scoped to.
- **Credential-socket hardening.** `devcontainer.json` clears `SSH_AUTH_SOCK`, `GPG_AGENT_INFO`, `GIT_ASKPASS` so host credentials don't leak in via VS Code's default forwarding. The post-start script also deletes any leftover credential sockets in `/tmp`.
