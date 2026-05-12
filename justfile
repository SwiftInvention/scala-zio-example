set dotenv-load := true

# Activates JDK & sbt versions from .sdkmanrc when SDKMAN is installed.
# No-op when SDKMAN isn't present (CI provides JDK/sbt via setup actions),
# so recipes work in both environments without branching.
init_env := '''
    export SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
    if [ -f "$SDKMAN_DIR/bin/sdkman-init.sh" ]; then
      set +u
      source "$SDKMAN_DIR/bin/sdkman-init.sh" >/dev/null
      sdk env >/dev/null
      set -u
    fi
'''

_default:
  @ just --list --unsorted

# ── Setup ─────────────────────────────────────────────────────────────────────

# install JDK, sbt (SDKMAN), markdownlint-cli2 (npm), and seed local config from the .example
[group('setup')]
initial-setup:
  #!/usr/bin/env bash
  set -eu
  export SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
  source "$SDKMAN_DIR/bin/sdkman-init.sh"
  sdk env install
  if ! command -v npm >/dev/null 2>&1; then
    echo "✗ npm not found on PATH — install Node (e.g. via nvm) before running initial-setup" >&2
    exit 1
  fi
  npm install -g markdownlint-cli2@0.22.1
  for example in modules/*/src/main/resources/application-*.conf.example .env.example; do
    target="${example%.example}"
    if [ ! -f "$target" ]; then
      cp "$example" "$target"
      echo "Created $target from template"
    fi
  done

# ── Dev loop ──────────────────────────────────────────────────────────────────

# compile main and test sources (warnings as errors)
[group('dev loop')]
compile:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "ci; compile; Test / compile"

# run tests (excludes integration tests — for those run `just test-it`)
[group('dev loop')]
test:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; unitTest"

# integration tests; silent by default, pass a level (trace|debug|info|warn|error) to see logs. On host, brings up the dedicated `:3307` test mysql via docker compose first. In the devcontainer the inline `mysql-test` is already up and migrated via initdb, so the docker-dance is skipped.
[group('dev loop')]
test-it level='':
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  if [ -z "${DEVCONTAINER:-}" ]; then
    just test-infra-reset
    just test-db-migrate
  fi
  export TEST_LOG_LEVEL='{{ level }}'
  sbt "dev; it/test"

# lint, check format (warnings as errors). Covers Scala (sbt) + Markdown (markdownlint-cli2).
[group('dev loop')]
style-check:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "ci; compile; Test / compile; styleCheck"
  markdownlint-cli2

# lint with autofixes, format. Covers Scala (sbt) + Markdown (markdownlint-cli2 --fix).
[group('dev loop')]
style-fix:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; styleFix"
  markdownlint-cli2 --fix

# style-fix + style-check + unit tests in one sbt session — run before committing (skips integration tests)
[group('dev loop')]
precommit-fix:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; styleFix; ci; styleCheck; unitTest"
  markdownlint-cli2 --fix
  just test-it

# ── Run ───────────────────────────────────────────────────────────────────────

# run server in foreground (Ctrl+C to stop)
[group('run')]
run:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; appServer/run"

# run a development scratchpad (modules/app/dev/.../Experiment.scala) against local MySQL
[group('run')]
experiment:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; appDev/run"

# seed example fixtures (customers + notifications) into local MySQL
[group('run')]
seed-example:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; appDev/runMain com.example.app.dev.actions.SeedExample"

# curl the running server's endpoints (success + typed-error 404). assumes server up on :8080
[group('run')]
smoke-test:
  #!/usr/bin/env bash
  set -eu

  echo "GET /health (liveness — always 200, no DB):"
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health)
  echo "HTTP $code"
  if [ "$code" != "200" ]; then
    echo "✗ expected 200, got $code"; exit 1
  fi

  echo
  echo "GET /ready (readiness — DB ping, expect 200):"
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ready)
  echo "HTTP $code"
  if [ "$code" != "200" ]; then
    echo "✗ expected 200, got $code"; exit 1
  fi

  echo
  echo "GET /docs (Swagger UI HTML, expect 200):"
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/docs)
  echo "HTTP $code"
  if [ "$code" != "200" ]; then
    echo "✗ expected 200, got $code"; exit 1
  fi

  echo
  echo "GET /docs/scala-zio-example.json (OpenAPI spec, expect 200):"
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/docs/scala-zio-example.json)
  echo "HTTP $code"
  if [ "$code" != "200" ]; then
    echo "✗ expected 200, got $code"; exit 1
  fi

  echo
  echo "GET /customers:"
  curl -sSf http://localhost:8080/customers | jq .

  echo
  echo "GET /customers/c-001 (existing):"
  curl -sSf http://localhost:8080/customers/c-001 | jq .

  echo
  echo "GET /customers/missing (typed-error path, expect 404 with ErrorTO body):"
  resp=$(curl -s -w "\n%{http_code}" http://localhost:8080/customers/missing)
  body=$(echo "$resp" | sed '$d')
  code=$(echo "$resp" | tail -1)
  echo "HTTP $code"
  echo "$body" | jq .
  if [ "$code" != "404" ]; then
    echo "✗ expected 404, got $code"; exit 1
  fi

  echo
  echo "GET /notifications (list with embedded recipient from customer ctx):"
  curl -sSf http://localhost:8080/notifications | jq .

  echo
  echo "GET /customers/c-001/notifications (notifications scoped to one customer):"
  curl -sSf http://localhost:8080/customers/c-001/notifications | jq .

  echo
  echo "POST /notifications (cross-ctx existence check passes — recipient c-001 exists):"
  curl -sSf -X POST http://localhost:8080/notifications \
    -H "Content-Type: application/json" \
    -d '{"recipientId":"c-001","channel":"Email","message":"hello from smoke test"}' | jq .

  echo
  echo "POST /notifications with missing recipient (expect 404 propagated from customer ctx):"
  resp=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/notifications \
    -H "Content-Type: application/json" \
    -d '{"recipientId":"c-missing","channel":"Email","message":"this should 404"}')
  body=$(echo "$resp" | sed '$d')
  code=$(echo "$resp" | tail -1)
  echo "HTTP $code"
  echo "$body" | jq .
  if [ "$code" != "404" ]; then
    echo "✗ expected 404, got $code"; exit 1
  fi

  echo
  echo "✅ smoke test passed"

# ── Dev infra ─────────────────────────────────────────────────────────────────

# bring up local infra: MySQL on :3306, Jaeger on :4318 (OTLP HTTP) + :16686 (UI). blocks until healthy.
[group('dev infra')]
local-infra-up:
  docker compose up -d --wait mysql jaeger

# stop local infra (preserves MySQL volume; Jaeger has no volume)
[group('dev infra')]
local-infra-down:
  docker compose stop mysql jaeger

# wipe local infra state (drops MySQL data, recreates Jaeger to clear in-memory traces) and bring it back up
[group('dev infra')]
local-infra-reset:
  docker compose down -v mysql jaeger
  just local-infra-up

# apply Flyway migrations to local MySQL (uses lib/common/.../db/migration). `MYSQL_HOST` comes from .env (localhost on host, mysql in the devcontainer).
[group('dev infra')]
db-migrate:
  flyway \
    -url="jdbc:mysql://${MYSQL_HOST}:3306/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# ── Test infra ────────────────────────────────────────────────────────────────

# bring up integration-test MySQL on :3307 (blocks until healthy). Separate compose project from dev infra.
[group('test infra')]
test-infra-up:
  docker compose -p scala-zio-example-test -f docker-compose.test.yml up -d --wait mysql

# stop integration-test MySQL (preserves volume)
[group('test infra')]
test-infra-down:
  docker compose -p scala-zio-example-test -f docker-compose.test.yml stop mysql

# wipe integration-test MySQL data and bring it back up
[group('test infra')]
test-infra-reset:
  docker compose -p scala-zio-example-test -f docker-compose.test.yml down -v
  just test-infra-up

# apply Flyway migrations to test MySQL (port 3307)
[group('test infra')]
test-db-migrate:
  flyway \
    -url="jdbc:mysql://localhost:3307/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# ── Docker ────────────────────────────────────────────────────────────────────

# build the server docker image as `scala-zio-example-server:<version>` and `:latest` via sbt-native-packager
[group('docker')]
docker-build:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "appServer/Docker/publishLocal"

# bring up the dockerized server alongside infra (uses the most recent `docker-build` output). blocks until healthy.
[group('docker')]
docker-run:
  docker compose up -d --wait server

# stop the dockerized server (preserves infra)
[group('docker')]
docker-stop:
  docker compose stop server

# ── End-to-end ────────────────────────────────────────────────────────────────

# from any prior state, get a working local server foreground (local-infra-reset, db-migrate, seed, run)
[group('end-to-end')]
start-fresh-local-server: local-infra-reset db-migrate seed-example run

# from any prior state, get a working dockerized server (local-infra-reset, db-migrate, seed, docker-build, docker-run)
[group('end-to-end')]
start-fresh-docker-server: local-infra-reset db-migrate seed-example docker-build docker-run
