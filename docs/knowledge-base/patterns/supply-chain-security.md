# Supply Chain Security

Practices that shrink the blast radius of a compromised upstream.

## Threat model

What we're defending against:

- **Malicious dependency** published under a typosquat[^2], a hijacked maintainer account, or an auto-mirror republishing artifacts from another package ecosystem.
- **Mutated transitive** — a version we resolve today differs from what we resolved yesterday, without any change to our build files.
- **Compromised build-time code** — a malicious sbt plugin, source generator, or macro running with full filesystem and network access during `compile`.

## The build is code

`build.sbt`, everything under `project/`, and every sbt plugin executes Scala with full host privileges during `compile`. So do macros: a macro implementation in a transitive dep runs at compile time, with the build host's filesystem and network. The sbt team's framing[^1]: *"building a project carries an inherent risk of arbitrary command execution."*

Practices:

- **Pin plugin versions.** Exact versions only — no `latest.release`, no version ranges. All plugins in `project/plugins.sbt` use `% "x.y.z"`.
- **Pin sbt itself.** `project/build.properties` sets `sbt.version=<exact>` so the launcher can't silently fetch a different sbt.
- **Review build changes like production code.** A PR that bumps `project/plugins.sbt` or adds a `sourceGenerator` is in the same risk class as a PR that touches application logic. No drive-by "just bump the build plugin" merges.
- **No source dependencies.** `ProjectRef(uri("git://..."))`, `dependsOn(RootProject(uri(...)))`, or `ProjectRef(uri("https://..."))` pull and execute remote Scala. Banned outright.
- **Choose macro-heavy deps deliberately.** Quill, enumeratum, zio-schema-derivation all run macros at compile time. The library set is small and well-known; new additions go through review.
- **Global plugins file is empty.** `~/.sbt/1.0/plugins/` runs on every build on the machine — a global plugin compromises every project. The devcontainer ships with an empty global plugins directory. On host, audit `~/.sbt/1.0/plugins/`; anything there should be a deliberate, reviewed choice.

## Artifact sources

`resolvers` is the set of places sbt is willing to fetch from. Expanding it expands the trust set.

- **Default resolvers only.** Maven Central + the sbt-plugins repo. Nothing else is configured in `build.sbt` or `~/.sbt/`.
- **No JitPack.** JitPack[^4] builds artifacts on demand from GitHub repos with no signature, no namespace verification beyond GitHub usernames (which can be abandoned and re-claimed), and an opaque build environment.
- **Treat auto-mirrors as the source ecosystem.** A `groupId` whose business is republishing artifacts from elsewhere inherits the source ecosystem's risk surface. `org.mvnpm:*` (npm packages republished as Maven coords) was the carrier for the November 2025 Shai-Hulud spillover into Central[^3].
- **If you need internal artifacts**, proxy through a single Nexus/Artifactory "group" repo with namespace routing rules — never chain "internal first, Central fallback" resolvers. Coursier picks the highest version across resolvers, not the first that responds; a public package with a higher version wins against your internal one (the dependency-confusion attack[^5]).

## Lockfile

sbt has no native lockfile — Coursier re-resolves every transitive on every clean build, picking the newest in-range version each time. [`sbt-dependency-lock`](https://stringbean.github.io/sbt-dependency-lock/) pins the resolved graph: one `build.sbt.lock` per module, with SHA-1 hashes per artifact.

Workflow (recipes documented in [`commands.md`](../commands.md)):

```sh
just deps-relock          # regenerate all lockfiles after intentional dep changes
sbt dependencyLockCheck   # fail if resolved graph differs from lock
```

`dependencyLockCheck` is wired into `just style-check` (CI gate) and `just precommit-fix` (done-state gate), and runs before `compile` so a mutated transitive's macros don't execute before the check fires. A PR that bumps a dep without regenerating the lock fails the check; a PR that mutates the lock without touching `project/Versions.scala` (the version-declaration file) is reviewable as a deliberate change.

This catches three things:

1. A transitive resolving to a new version because some intermediate dep widened its range.
2. A repo serving a different artifact for the same coords (compromised mirror, MITM).
3. A non-deterministic build (different machine resolves a different graph).

## Cooldown

Newly published versions get pulled before the community notices the compromise. Coursier has no built-in cooldown, so the gate lives at the changeset that proposes the dep change.

**Custom check via `scripts/deps-cooldown-check.sh`** (exposed as `just deps-cooldown <days>`). For every coord in every `build.sbt.lock` plus every sbt/compiler plugin in `project/plugins.sbt`, the script HEADs `https://repo1.maven.org/maven2/<g>/<a>/<v>/`, reads the `Last-Modified` header, and fails if the artifact is younger than `<days>`. Publish dates are immutable, so successful lookups are cached forever under `.cache/deps-cooldown/` (gitignored, repo-local) — only newly-added or newly-bumped coords need a network hit on subsequent runs.

```sh
just deps-cooldown 7        # check against a 7-day window
just deps-cooldown 14       # tighter window for one-off audit
```

The check is wired into `just style-check` (CI gate) and `just precommit-fix` (done-state gate) with the project's chosen window (7 days) declared at each call site. A PR that adds or bumps to a too-young version fails the gate; you wait, or pin to an older version.

Coverage:

- **Main-build deps** — all artifacts in `build.sbt.lock` files, transitives included. Intra-project modules (artifacts: []) are skipped.
- **sbt plugins + compiler plugins** — `addSbtPlugin` and `addCompilerPlugin` lines in `project/plugins.sbt`. The build-time arbitrary-code surface is the highest-risk class; cooldown matters most here.

Out of scope:

- **GitHub Actions** — different lookup mechanism. Pinned to commit SHAs separately; see "CI workflow actions" below.
- **JDK and sbt itself** — pinned via `.sdkmanrc` and `project/build.properties`; not Maven-Central-resolved.

No Scala Steward or Renovate in the loop — deps change by human PR, and the gate covers both initial adds and bumps.

## CI workflow actions

Actions referenced in `.github/workflows/*.yml` run on the CI runner with access to `GITHUB_TOKEN` and any workflow-scoped secrets — a compromised release can exfiltrate or alter the build. Unlike Maven Central coordinates, GitHub tags aren't immutable: a maintainer (or an attacker with maintainer credentials) can move `@v4.3.1` to point at different code without bumping the version.

Pin every action to its full 40-character commit SHA. Keep the tag as a trailing comment so the version stays legible in review:

```yaml
- uses: actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5 # v4.3.1
```

SHAs are immutable; the workflow runs exactly the code reviewed when the pin was set. Bumping an action means resolving the new tag's SHA, reviewing the diff between old and new SHA (not just the tag's release notes), and updating both the pin and the comment in one commit.

Coverage: every `uses:` clause across `.github/workflows/` resolves to a SHA. The trailing tag comment is documentation only — GitHub ignores it at run time.

The pin fixes the action's source, not what that source does at run time. An action's body can fetch external resources mid-run — `setup-node` downloads Node binaries from a CDN, `actions/cache` reads and writes a GitHub-managed blob store, container actions pull images that may themselves reference mutable tags. Vetting that runtime behavior is a separate review.

## Dependency surface

The smaller the trusted compute base, the smaller the attack surface. Two practices:

**Don't depend on abandoned upstreams.** When a dep is no longer maintained, flag it inline at its declaration site in `project/Versions.scala` with a `// Note: ...` marker — each flagged dep is a candidate for replacement.

**Each module declares what it uses.** [`sbt-explicit-dependencies`](https://github.com/cb372/sbt-explicit-dependencies) runs in CI as a build-failing gate:

- **`undeclaredCompileDependenciesTest`** — fails if a module's source imports a library not in its `libraryDependencies`. The transitive's version would otherwise be implicit (whatever some other dep happens to pull); failing the build forces a deliberate declaration with a deliberate version.
- **`unusedCompileDependenciesTest`** — fails if `libraryDependencies` lists something nothing imports. Dead surface — drop it.

The build is structured so each module's `libraryDependencies` is the truth about what that module touches at compile time. Ctx modules redeclare cross-cutting types (`zio-prelude`, `zio-schema`) directly, rather than picking them up transitively through the shared `libCommon` module.

Two narrow filter sets cover what the plugin can't see through (configured in `build.sbt` under `explicitDepsFilters`):

- **Runtime-only artifacts** (`unusedCompileDependenciesFilter`) — JDBC drivers, slf4j bridges, the plain `quill-jdbc` shim. Required on the runtime classpath; never imported in Scala source. The plugin does compile-time bytecode analysis only and can't see them.
- **Umbrella sub-artifacts** (`undeclaredCompileDependenciesFilter`) — bytecode references to sub-artifacts pulled by a declared umbrella (`pureconfig-core` via `pureconfig`, `quill-core` / `quill-engine` via `quill-jdbc-zio`, ZIO type-tag / fiber-trace internals via `zio`).

Each filter entry is documented inline at its declaration site. Adding a new runtime-only dep or umbrella means adding a filter line.

## SBOM

[`sbt-sbom`](https://github.com/sbt/sbt-sbom) emits a CycloneDX SBOM via `sbt appServer/makeBom` (exposed locally as `just sbom`), scoped to `appServer` (the deployable unit). CI runs it on every push to `main` and uploads `modules/app/server/target/server-<version>.bom.json` as a workflow artifact.

The use case: when CVE-N is disclosed against `foo:bar:1.2.3` six months from now, the SBOM archive is a precise per-commit record of what was shipped, queryable via `jq` against any past build.

## Local development

Two practices that limit blast radius when an AI coding agent runs on a developer machine:

- **Devcontainer.** Dev work happens in a sandboxed VS Code Dev Container with WireGuard-tunneled outbound and mitmproxy-injected secrets. Malicious code reaching the build classpath can only exfiltrate to allow-listed hosts. See [`devcontainer.md`](devcontainer.md).
- **Keep secrets out of `.env` files and build env.** This template ships local credentials in `application-local.conf` (matching `docker-compose.yml`); production deployments should use a secrets manager (1Password CLI, Infisical, Vault — operators choose) and inject just-in-time at the running process (`op run -- <server>` or equivalent). The build itself never runs under the secrets-manager wrapper, so production secrets are never present in sbt's process env even when compile-time macros read `System.getenv`.

## Where this lives in the repo

| Concern              | Mechanism                                      | Surface                        |
| -------------------- | ---------------------------------------------- | ------------------------------ |
| Plugin pinning       | `project/plugins.sbt` exact versions           | reviewable in PR diff          |
| sbt pinning          | `project/build.properties`                     | reviewable in PR diff          |
| No source deps       | convention; no `ProjectRef(uri(...))` anywhere | review catches additions       |
| Lockfile             | `sbt-dependency-lock` → `build.sbt.lock`       | `dependencyLockCheck` in CI    |
| Cooldown             | `scripts/deps-cooldown-check.sh`               | `just deps-cooldown` in CI gate |
| Action pinning       | full SHA + tag-as-comment in `.github/workflows/` | reviewable in PR diff       |
| Dependency surface   | `sbt-explicit-dependencies`                    | `(un)declaredCompileDependenciesTest` in CI |
| SBOM                 | `sbt-sbom` → `makeBom`                         | CI artifact on `main`          |
| Sandbox              | `.devcontainer/` + `.sandcat/`                 | see [`devcontainer.md`](devcontainer.md) |

[^1]: [Scala Center: Fixing a Command Injection Vulnerability in sbt](https://www.scala-lang.org/blog/2026/03/31/sbt-security-advisory.html) (March 2026). Covers both the framing quote and CVE-2026-32948.
[^2]: [Aikido — Maven Central Jackson typosquatting malware](https://www.aikido.dev/blog/maven-central-jackson-typosquatting-malware) (December 2025).
[^3]: [GitGuardian — Shai-Hulud 2](https://blog.gitguardian.com/shai-hulud-2/) (November 2025). The Maven Central propagation went through the `org.mvnpm` mirror.
[^4]: [committing-crimes.com — jitpack.io, Dangerously Simple](https://committing-crimes.com/articles/2024-09-09-jitpack/) (September 2024).
[^5]: [Sonatype — Namespace Confusion Protection](https://help.sonatype.com/en/namespace-confusion-protection.html).
