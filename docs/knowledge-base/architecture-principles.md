# Architecture Principles

Architectural commitments the codebase adheres to.

Adding a principle here requires that every existing violation is fixed, marked `FIXME` (intent to fix), or marked `WONTFIX` (with reason — legacy too painful, code slated for deletion, etc.). New code introduces no unmarked violations.

Inline markers go next to the offending code, referencing the principle by its slug:

```scala
// architecture: bounded-context FIXME
```

---

## `module-layout` — three module types: lib, ctx, app

Modules live under `modules/{lib,ctx,app}/`. Each layer has a fixed internal shape. sbt project IDs follow `<layer><CamelCaseName>`.

See [`patterns/module-layout.md`](patterns/module-layout.md).

## `bounded-context` — each context is a 2-module pair with domain/app/impl internals

A context is `<ctx>` + `<ctx>-api`. Inside the impl module: `domain/` (abstractions), `app/` (the orchestrator trait + its impl, colocated), `impl/` (concrete adapters; `impl/service/` mirrors `domain/service/`).

See [`patterns/bounded-context.md`](patterns/bounded-context.md).

## `ctx-api` — cross-context calls go through `<ctx>-api`, never directly

The trait + TOs live in `<ctx>-api`. The DirectClient impl lives in the ctx module and owns TO ↔ domain conversion. HTTP routes are a separate plane and don't go through the cross-context trait.

See [`patterns/ctx-api.md`](patterns/ctx-api.md).

## `build-deps` — module dependencies enforce the architecture

`build.sbt`'s `dependsOn` is compile-time enforcement of every cross-module boundary. Naming conventions and folder layouts are conventions; the build graph is the rule.

See [`patterns/build-deps.md`](patterns/build-deps.md).

## `import-direction` — imports flow `impl → app → domain`

Within and across modules: `impl/` files may import from `app/` and `domain/`; `app/` files may import from `domain/`; `domain/` files import from neither. Convention-only — the build can't enforce this. Review catches it.

See [`patterns/build-deps.md`](patterns/build-deps.md#convention-only-impl--app--domain-import-direction).

## `errors` — typed errors via `AppFailure`, flowing through the `Throwable` channel

Trait signatures stay `AppIO` (Throwable). Concrete errors are `AppFailure` subclasses carrying `category`, `reason`, and HTTP status. The route boundary renders any `AppFailure` as a structured `ErrorTO`; unknown throwables get wrapped as `UnhandledApiError` (500).

See [`patterns/errors.md`](patterns/errors.md).

## `to-converters` — TO ↔ domain mapping in dedicated converter objects

One `<Entity>Converter` object per entity in `<ctx>/impl/to/converter/`. Hand-written, no chimney. Methods named `to<Entity>TO` and `to<Entity>`.

See [`patterns/converters.md`](patterns/converters.md).

## `pe-layout` — persistence entities live with their owning ctx, not in lib

PEs (`<Entity>PE`) live in `<ctx>/impl/service/repo/pg/entity/`, alongside the repo impl that uses them. They don't leak past `impl/` — repo trait signatures use domain types only. Genuinely cross-cutting PEs (audit log, outbox) go in `lib/common/impl/repo/pg/entity/`.

See [`patterns/persistence.md`](patterns/persistence.md).

## `pe-converters` — PE ↔ domain mapping in dedicated converter objects

Mirrors `to-converters` for the persistence boundary. One `<Entity>PEConverter` object per PE in `<ctx>/impl/service/repo/pg/converter/`. Hand-written. Methods named `to<Entity>` and `to<Entity>PE`.

See [`patterns/persistence.md`](patterns/persistence.md).

## `tx-default` — repo methods open transactions; app services may wrap

Every repo method wraps its query in `Transactor.withTransaction`. App-service methods that orchestrate multiple repo calls may wrap the orchestration in another `withTransaction` — Quill's transaction is reentrant on a fiber-local connection, so nesting reuses the outer scope.

See [`patterns/persistence.md`](patterns/persistence.md).

## `newtypes` — domain ids and constrained value objects are zio-prelude `Newtype`s

Domain identifiers (`CustomerId`) and constrained value objects (`Email`) flow through the system as zio-prelude `Newtype`s, never as raw `String`/`Int`. The compiler catches argument swaps; smart constructors fail at construction for invalid input; serialization stays flat (no wrapping objects on the wire).

Ids are centralized in `lib/common/.../domain/model/NewTypes.scala`. Value objects with smart constructors live with the ctx that owns them, because their validation logic is domain-specific.

See [`patterns/newtypes.md`](patterns/newtypes.md).

## `config-shape` — typed config, per-(app, env) files, no defaults in code

Config is loaded into PureConfig case classes that fail-fast at boot. Files are per-(app, env) and self-contained — no `reference.conf`, no overlay between envs. The active env file is selected by `APP_ENV`. Each module owns its own `XConfig` (no central root config).

**No default values in code, anywhere.** Not on case-class fields, not in `getOrElse` fallbacks, not in `if/else` branches that produce a fallback value. Required fields are required; conceptually-optional fields are `Option[X]` and the consumer must branch on present/absent semantically (not substitute a baked-in value). The runtime value of any config key is fully determined by the active `.conf` file.

See [`patterns/config.md`](patterns/config.md).
