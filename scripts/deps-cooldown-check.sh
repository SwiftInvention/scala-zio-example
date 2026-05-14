#!/usr/bin/env bash
# Fail if any declared dependency is younger than COOLDOWN_DAYS.
#
# Scope:
#   - main-build deps: every artifact in every `build.sbt.lock` (one per module)
#   - sbt plugins + compiler plugins: parsed from `project/plugins.sbt`
#
# Lookup mechanism: HEAD `https://repo1.maven.org/maven2/<org>/<name>/<version>/`
# and read the `Last-Modified` header. Publish dates are immutable, so successful
# lookups are cached forever in $CACHE_DIR.
#
# See docs/knowledge-base/patterns/supply-chain-security.md for rationale.

set -euo pipefail

# Run from the repo root so the relative cache + lockfile paths resolve.
cd "$(dirname "$0")/.."

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <cooldown-days>" >&2
  echo "  e.g. $0 7" >&2
  exit 2
fi
if ! [[ "$1" =~ ^[0-9]+$ ]]; then
  echo "ERROR: cooldown-days must be a non-negative integer, got '$1'" >&2
  exit 2
fi
COOLDOWN_DAYS="$1"

CACHE_DIR="${COOLDOWN_CACHE_DIR:-.cache/deps-cooldown}"
mkdir -p "$CACHE_DIR"

# Cross-version suffixes for plugin-style coords. Stable for this template's
# (sbt 1.x, Scala 2.13) toolchain; bump these alongside the toolchain.
SBT_PLUGIN_CROSS="_2.12_1.0"
SCALA_BINARY_CROSS="_2.13"
SCALA_FULL_CROSS="_2.13.18"

cooldown_secs=$(( COOLDOWN_DAYS * 86400 ))
now=$(date +%s)
threshold=$(( now - cooldown_secs ))

# ── Date parsing: macOS BSD vs Linux GNU ─────────────────────────────────────
# Last-Modified is RFC-1123: "Thu, 04 Jul 2024 09:10:12 GMT"
parse_http_date() {
  local s="$1"
  if date -j -f "%a, %d %b %Y %H:%M:%S %Z" "$s" +%s 2>/dev/null; then
    return 0
  fi
  if date -d "$s" +%s 2>/dev/null; then
    return 0
  fi
  return 1
}

# ── Coord collection ─────────────────────────────────────────────────────────

declare -A coords  # key "org|name|version" → 1; dedups across lockfiles

# Main-build coords from every lockfile. Intra-project modules are recorded
# with an empty `artifacts` array (they don't publish jars); filter them out.
while IFS=$'\t' read -r org name version; do
  [[ -z "$org" ]] && continue
  coords["${org}|${name}|${version}"]=1
done < <(
  find . -name "build.sbt.lock" -not -path "*/target/*" -not -path "*/.bloop/*" -print0 \
    | xargs -0 -I {} jq -r '.dependencies[] | select(.artifacts | length > 0) | "\(.org)\t\(.name)\t\(.version)"' {}
)

# Plugin coords from project/plugins.sbt — resolve val-named versions
plugins_sbt="project/plugins.sbt"
declare -A val_to_version
while IFS= read -r line; do
  if [[ "$line" =~ ^[[:space:]]*val[[:space:]]+([A-Za-z0-9_]+)[[:space:]]*=[[:space:]]*\"([^\"]+)\" ]]; then
    val_to_version["${BASH_REMATCH[1]}"]="${BASH_REMATCH[2]}"
  fi
done < "$plugins_sbt"

# addSbtPlugin / addCompilerPlugin lines. Strip trailing line-comments first.
while IFS= read -r raw; do
  line="${raw%%//*}"

  # sbt plugin: addSbtPlugin("org" % "name" % versionVal)
  if [[ "$line" =~ addSbtPlugin\([[:space:]]*\"([^\"]+)\"[[:space:]]*%[[:space:]]*\"([^\"]+)\"[[:space:]]*%[[:space:]]*([A-Za-z0-9_]+) ]]; then
    org="${BASH_REMATCH[1]}"
    name="${BASH_REMATCH[2]}${SBT_PLUGIN_CROSS}"
    version="${val_to_version[${BASH_REMATCH[3]}]:-}"
    [[ -z "$version" ]] && { echo "WARN: unresolved version val '${BASH_REMATCH[3]}' for sbt plugin '${BASH_REMATCH[1]}:${BASH_REMATCH[2]}'" >&2; continue; }
    coords["${org}|${name}|${version}"]=1
    continue
  fi

  # Compiler plugin with `cross CrossVersion.full`: addCompilerPlugin("org" % "name" % v cross CrossVersion.full)
  if [[ "$line" =~ addCompilerPlugin\([[:space:]]*\"([^\"]+)\"[[:space:]]*%[[:space:]]*\"([^\"]+)\"[[:space:]]*%[[:space:]]*([A-Za-z0-9_]+)[[:space:]]+cross[[:space:]]+CrossVersion\.full ]]; then
    org="${BASH_REMATCH[1]}"
    name="${BASH_REMATCH[2]}${SCALA_FULL_CROSS}"
    version="${val_to_version[${BASH_REMATCH[3]}]:-}"
    [[ -z "$version" ]] && { echo "WARN: unresolved version val '${BASH_REMATCH[3]}' for compiler plugin '${BASH_REMATCH[1]}:${BASH_REMATCH[2]}'" >&2; continue; }
    coords["${org}|${name}|${version}"]=1
    continue
  fi

  # Compiler plugin with %% (binary cross-version): addCompilerPlugin("org" %% "name" % v)
  if [[ "$line" =~ addCompilerPlugin\([[:space:]]*\"([^\"]+)\"[[:space:]]*%%[[:space:]]*\"([^\"]+)\"[[:space:]]*%[[:space:]]*([A-Za-z0-9_]+) ]]; then
    org="${BASH_REMATCH[1]}"
    name="${BASH_REMATCH[2]}${SCALA_BINARY_CROSS}"
    version="${val_to_version[${BASH_REMATCH[3]}]:-}"
    [[ -z "$version" ]] && { echo "WARN: unresolved version val '${BASH_REMATCH[3]}' for compiler plugin '${BASH_REMATCH[1]}:${BASH_REMATCH[2]}'" >&2; continue; }
    coords["${org}|${name}|${version}"]=1
    continue
  fi
done < "$plugins_sbt"

# ── Check each coord against Maven Central ───────────────────────────────────

violations=()
checked=0
cached=0
fetched=0
warned=0

for coord in "${!coords[@]}"; do
  IFS='|' read -r org name version <<< "$coord"
  checked=$((checked + 1))

  # Cache key: safe filename derived from the coord
  cache_key=$(echo "${org}_${name}_${version}" | tr '/.: ' '____')
  cache_file="${CACHE_DIR}/${cache_key}"

  if [[ -f "$cache_file" ]]; then
    publish_epoch=$(<"$cache_file")
    cached=$((cached + 1))
  else
    org_path="${org//.//}"
    url="https://repo1.maven.org/maven2/${org_path}/${name}/${version}/"
    last_modified=$(curl -sI "$url" | awk -F': ' 'tolower($1)=="last-modified"{print $2}' | tr -d '\r\n')
    if [[ -z "$last_modified" ]]; then
      echo "WARN: no Last-Modified for ${coord} (${url}) — skipping" >&2
      warned=$((warned + 1))
      continue
    fi
    publish_epoch=$(parse_http_date "$last_modified" || true)
    if [[ -z "$publish_epoch" ]]; then
      echo "WARN: couldn't parse '${last_modified}' for ${coord} — skipping" >&2
      warned=$((warned + 1))
      continue
    fi
    echo "$publish_epoch" > "$cache_file"
    fetched=$((fetched + 1))
  fi

  if (( publish_epoch > threshold )); then
    days_ago=$(( (now - publish_epoch) / 86400 ))
    violations+=("${org}:${name}:${version} — published ${days_ago}d ago (< ${COOLDOWN_DAYS}d cooldown)")
  fi
done

echo "Cooldown check: ${checked} coords inspected (${cached} cached, ${fetched} fetched, ${warned} warned)" >&2

if (( ${#violations[@]} > 0 )); then
  echo "" >&2
  echo "FAIL: ${#violations[@]} dep(s) younger than the ${COOLDOWN_DAYS}-day cooldown:" >&2
  for v in "${violations[@]}"; do
    echo "  - $v" >&2
  done
  echo "" >&2
  echo "Either wait for the cooldown window to elapse, or pin to an older version." >&2
  exit 1
fi
