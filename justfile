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
