# Build Dependencies

`build.sbt`'s `dependsOn` clauses determine what each module can `import`. The compile-time dep graph **is** the architectural boundary — not naming conventions, not docs. If you want a boundary the codebase actually respects, this is the only one with teeth.

## In `build.sbt`

```scala
lazy val libCommon = (project in file("modules/lib/common"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioPreludeDep ++ zioJsonDep)

lazy val ctxCustomerApi = (project in file("modules/ctx/customer-api"))
  .dependsOn(libCommon)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioJsonDep)

lazy val ctxCustomer = (project in file("modules/ctx/customer"))
  .dependsOn(libCommon, ctxCustomerApi)
  .settings(commonSettings)
  .settings(libraryDependencies ++= zioCoreDep ++ zioHttpDep ++ zioJsonDep ++ dbDep)

lazy val appServer = (project in file("modules/app/server"))
  .dependsOn(libCommon, ctxCustomerApi, ctxCustomer)
  .settings(...)
```

## Rules

- **Libs depend on libs only.** Never on a ctx or an app.
- **`<ctx>-api` modules** depend only on libs. No ctx impls. No other `-api` modules.
- **`<ctx>` modules** depend on libs, on their own `<ctx>-api`, and on *other* contexts' `<ctx>-api` (when calling them cross-context). Never on another ctx's impl module.
- **`<app>` modules** depend on everything they compose: libs, all relevant `-api` modules, all relevant ctx impl modules.

## Convention-only: `impl → app → domain` import direction

Beyond what the build enforces, there's a convention about which way imports flow inside the codebase: `impl/` files may import from `app/` and `domain/`; `app/` files may import from `domain/`; `domain/` files import from neither. This holds inside a single context AND across ctx/lib boundaries — e.g., `lib/foo/domain/` shouldn't reach into `lib/bar/impl/`.

Why the build doesn't enforce it:

- **Within a single sbt module** — everything is on one classpath; the compiler doesn't see folder structure as a boundary.
- **Across sbt modules** — once module A `dependsOn` module B, A sees ALL of B's classes. sbt has `compile->compile` configurations that could partition this, but it's not worth the ceremony.

So this is a convention enforced by reading, not by the compiler. Watch for it in review. Common slip: routes (`impl/http/`) reaching into another context's `impl/` package directly instead of going through its `<ctx>-api`.

## Library dep groups

External library deps live in `project/Dependencies.scala` as named groups:

- `zioCoreDep`, `zioHttpDep`, `zioJsonDep`, `zioPreludeDep`
- `tapirDep`, `pureconfigDep`
- `dbDep` (quill, mysql; Flyway runs out-of-process via the CLI)
- `loggingDep` + `logExcludeDep` (zio-logging + slf4j bridges)
- `enumeratumDep`, `circeDep`, `newtypeDep`

Each module pulls in only what it needs. `libCommon` carries the cross-cutting deps (config, persistence, logging, telemetry, zio-http for the shared client + server pieces); `ctxCustomerApi` doesn't get DB deps; etc. Tighter deps per module make boundaries clearer at the import site.
