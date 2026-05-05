set dotenv-load := true

# Activates JDK & sbt versions from .sdkmanrc.
# Prepended to every public recipe so they don't depend on shell state.
init_env := '''
    export SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
    set +u
    source "$SDKMAN_DIR/bin/sdkman-init.sh" >/dev/null
    sdk env >/dev/null
    set -u
'''

_default:
  @ just --list --unsorted

# install JDK, sbt (SDKMAN), and seed local config from the .example
initial-setup:
  #!/usr/bin/env bash
  set -eu
  export SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
  source "$SDKMAN_DIR/bin/sdkman-init.sh"
  sdk env install
  for example in modules/*/src/main/resources/application-*.conf.example; do
    target="${example%.example}"
    if [ ! -f "$target" ]; then
      cp "$example" "$target"
      echo "Created $target from template"
    fi
  done

# compile main and test sources (warnings as errors)
compile:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "ci; compile; Test / compile"

# run tests (excludes integration tests — for those run `just test-it`)
test:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; unitTest"

# run integration tests against test MySQL (port 3307) — brings up + migrates the test container automatically
test-it: db-reset-test db-migrate-test
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; it/test"

# lint, check format (warnings as errors)
style-check:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "ci; compile; Test / compile; styleCheck"

# lint with autofixes, format
style-fix:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; styleFix"

# auto-fix style, then verify style + unit tests in one sbt session — run before committing
# Excludes integration tests because they need infra; run `just test-it` separately when relevant.
precommit-fix:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; styleFix; ci; styleCheck; unitTest"

# run server in foreground (Ctrl+C to stop)
run:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  export APP_ENV=local
  sbt "dev; appServer/run"

# start MySQL container (blocks until healthy)
db-up:
  docker compose up -d --wait mysql

# stop MySQL container (preserves data volume)
db-down:
  docker compose stop mysql

# tear down MySQL container and wipe data
db-reset:
  docker compose down -v mysql
  just db-up

# apply Flyway migrations to local MySQL (uses lib/common/.../db/migration)
db-migrate:
  flyway \
    -url="jdbc:mysql://localhost:3306/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# start integration-test MySQL container on port 3307 (blocks until healthy)
db-up-test:
  docker compose up -d --wait mysql-test

# tear down test MySQL container and wipe data
db-reset-test:
  docker compose down -v mysql-test
  just db-up-test

# apply Flyway migrations to test MySQL (port 3307)
db-migrate-test:
  flyway \
    -url="jdbc:mysql://localhost:3307/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# spin up server, hit GET /customers, tear down
smoke-test: db-up db-migrate
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  export APP_ENV=local

  log=$(mktemp)
  echo "Starting server (sbt run, backgrounded)..."
  sbt -no-colors "dev; appServer/run" > "$log" 2>&1 &
  SBT_PID=$!
  cleanup() {
    pkill -P "$SBT_PID" 2>/dev/null || true
    kill "$SBT_PID" 2>/dev/null || true
    pkill -f "com.example.app.server.ServerApp" 2>/dev/null || true
    wait 2>/dev/null || true
    rm -f "$log"
  }
  trap cleanup EXIT

  echo "Waiting for server to be reachable..."
  for i in $(seq 1 90); do
    if curl -sSf http://localhost:8080/customers > /dev/null 2>&1; then
      echo "✓ server up after ${i}s"
      break
    fi
    if ! kill -0 "$SBT_PID" 2>/dev/null; then
      echo "✗ sbt died before server came up. Last 30 lines of log:"
      tail -30 "$log"
      exit 1
    fi
    sleep 1
  done

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
  echo "✅ smoke test passed"
