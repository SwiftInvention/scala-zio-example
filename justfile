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

# install JDK & sbt versions pinned in .sdkmanrc
initial-setup:
  #!/usr/bin/env bash
  set -eu
  export SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
  source "$SDKMAN_DIR/bin/sdkman-init.sh"
  sdk env install

# compile main and test sources (warnings as errors)
compile:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "ci; compile; Test / compile"

# run tests
test:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; test"

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

# run server in foreground (Ctrl+C to stop)
run:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}
  sbt "dev; appServer/run"

# spin up server, hit GET /customers, tear down
smoke-test:
  #!/usr/bin/env bash
  set -eu
  {{ init_env }}

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
  echo "✅ smoke test passed"
