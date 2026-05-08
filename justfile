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

# integration tests against test MySQL (:3307); silent by default, pass a level (trace|debug|info|warn|error) to see logs
test-it level='': test-infra-reset test-db-migrate
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  export TEST_LOG_LEVEL='{{ level }}'
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

# style-fix + style-check + unit tests in one sbt session — run before committing (skips integration tests)
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

# run a development scratchpad (modules/app/dev/.../Experiment.scala) against local MySQL
experiment:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  export APP_ENV=local
  sbt "dev; appDev/run"

# seed the example customers (Ada, Alan, Grace) into local MySQL
seed-example:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  export APP_ENV=local
  sbt "dev; appDev/runMain com.example.app.dev.actions.SeedExampleCustomers"

# bring up local infra: MySQL on :3306, Jaeger on :4318 (OTLP HTTP) + :16686 (UI). blocks until healthy.
local-infra-up:
  docker compose up -d --wait mysql jaeger

# stop local infra (preserves MySQL volume; Jaeger has no volume)
local-infra-down:
  docker compose stop mysql jaeger

# wipe local infra state (drops MySQL data, recreates Jaeger to clear in-memory traces) and bring it back up
local-infra-reset:
  docker compose down -v mysql jaeger
  just local-infra-up

# apply Flyway migrations to local MySQL (uses lib/common/.../db/migration)
db-migrate:
  flyway \
    -url="jdbc:mysql://localhost:3306/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# bring up integration-test MySQL on :3307 (blocks until healthy)
test-infra-up:
  docker compose up -d --wait mysql-test

# stop integration-test MySQL (preserves volume)
test-infra-down:
  docker compose stop mysql-test

# wipe integration-test MySQL data and bring it back up
test-infra-reset:
  docker compose down -v mysql-test
  just test-infra-up

# apply Flyway migrations to test MySQL (port 3307)
test-db-migrate:
  flyway \
    -url="jdbc:mysql://localhost:3307/localDatabase" \
    -user=localUser \
    -password=localPassword \
    -locations="filesystem:modules/lib/common/src/main/resources/db/migration" \
    migrate

# from any prior state, get a working local server foreground (local-infra-reset, db-migrate, seed, run)
start-fresh-local-server: local-infra-reset db-migrate seed-example run

# curl the running server's endpoints (success + typed-error 404). assumes server up on :8080
smoke-test:
  #!/usr/bin/env bash
  set -eu

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
