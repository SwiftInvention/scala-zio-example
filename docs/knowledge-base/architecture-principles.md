# Architecture Principles

Architectural commitments the codebase adheres to.

Adding a principle here requires that every existing violation is fixed, marked `FIXME` (intent to fix), or marked `WONTFIX` (with reason — legacy too painful, code slated for deletion, etc.). New code introduces no unmarked violations.

Inline markers go next to the offending code, referencing the principle by its slug:

```scala
// architecture: bounded-context FIXME
```

Rules are revisable — flag tension if you see it.

---

The first two entries — `local-reasoning` and `correct-by-construction` — are meta-principles; the rest of this list are mechanisms for preserving them at one scale or another.

## `local-reasoning` — code is understandable from its signature plus its named dependencies

What a piece of code does should be derivable from reading that code plus its named dependencies — without descending into the bodies of those dependencies. Types, signatures, and explicit dependency parameters form the local frame; the discipline is that the frame is honest. Applies at every scale: expression, function, class, module, context, system.

Pattern: [`local-reasoning.md`](patterns/local-reasoning.md)

## `correct-by-construction` — make illegal states unrepresentable

Domain types are shaped so the compiler proves invariants the runtime never has to. Achieved through ADTs (legal state space modeled exactly), smart constructors (invariants enforced at the construction boundary), and parse-don't-validate (untyped input converted to typed values at every boundary). Functions operating on typed values trust the invariants without re-checking.

Pattern: [`correct-by-construction.md`](patterns/correct-by-construction.md).

---

## `module-layout` — three module types: lib, ctx, app

Modules live under `modules/{lib,ctx,app}/`. Each layer has a fixed internal shape. sbt project IDs follow `<layer><CamelCaseName>`; Scala packages mirror the layer with `com.example.<layer>.<name>.*`. Pattern: [`module-layout.md`](patterns/module-layout.md).

## `bounded-context` — each context is a 2-module pair with domain/app/impl internals

A context is `<ctx>` + `<ctx>-api`. Inside the impl module: `domain/` (abstractions), `app/` (the orchestrator trait + its impl, colocated), `impl/` (concrete adapters; `impl/service/` mirrors `domain/service/`). Pattern: [`bounded-context.md`](patterns/bounded-context.md).

## `import-direction` — imports flow `impl → app → domain`

Within and across modules: `impl/` may import from `app/` and `domain/`; `app/` may import from `domain/`; `domain/` imports from neither. Convention-only — review catches violations. Pattern: [`build-deps.md`](patterns/build-deps.md#convention-only-impl--app--domain-import-direction).

## `build-deps` — module dependencies enforce the architecture

`build.sbt`'s `dependsOn` is compile-time enforcement of every cross-module boundary. Naming and folder layouts are conventions; the build graph is the rule. Pattern: [`build-deps.md`](patterns/build-deps.md).

---

## `ctx-api` — cross-context calls go through `<ctx>-api`, never directly

The trait + TOs live in `<ctx>-api`. The DirectClient impl lives in the ctx module and owns TO ↔ domain conversion. HTTP routes are a separate plane and don't go through the cross-context trait. Pattern: [`cross-context-call.md`](patterns/cross-context-call.md).

## `to-converters` — TO ↔ domain mapping in dedicated converter objects

One `<Entity>Converter` object per entity in `<ctx>/impl/to/converter/`. Hand-written, no chimney. Methods named `to<Entity>TO` and `to<Entity>`. Pattern: [`converters.md`](patterns/converters.md).

---

## `newtypes` — domain ids are zio-prelude `Newtype`s, not raw `String`/`Int`

Identifiers (`CustomerId`, `AddressId`, `NotificationId`) flow through the system as typed wrappers — the compiler catches argument swaps, and serialization stays flat (no wrapping objects on the wire). Types live in `lib/common/.../domain/model/NewTypes.scala`; Quill `MappedEncoding`s live with the persistence infra in `lib/db/.../impl/sql/NewTypeEncodings.scala`. Pattern: [`newtypes.md`](patterns/newtypes.md).

## `smart-constructors` — validated value objects use `sealed abstract case class Foo private (...)`

Values with invariants (`Email`, `CustomerName`, `PostalCode`) construct through a smart-constructor `apply` that returns `AppIO[T]` and is the only path to a value. The triple `sealed abstract case class Foo private (...)` matters: naive `final case class Foo private (...)` leaks validation via the auto-generated `copy()`, so `abstract` is required to suppress it. Construction via `new Foo(...) {}` inside the companion. Value objects live with the owning ctx. Pattern: [`smart-constructors.md`](patterns/smart-constructors.md).

## `errors` — typed `AppFailure` channel, no implicit Throwable leakage

`AppIO[A]` is `IO[AppFailure, A]`. Every failure in the channel is a concrete `AppFailure` subclass carrying `category`, `reason`, and HTTP status. Raw JVM/JDBC `Throwable`s enter only via explicit `mapError` at the boundary (`Transactor`, `SqlContext`, `DataSourceLayer`). The route renders any `AppFailure` as a structured `ErrorTO` — no `Throwable` fallback. Pattern: [`errors.md`](patterns/errors.md).

## `config-shape` — typed config, per-(app, env) files, no defaults in code

PureConfig case classes loaded from per-(app, env) files at boot, fail-fast. Each file is its own truth — no `reference.conf`, no merge with another env's file. `APP_ENV` selects the file. Each module owns its `XConfig`; no central root.

Configurable values — anything an operator is meant to tune per env — live in the active `.conf` with no in-code fallback. External env vars override but carry no defaults of their own. Conceptually-optional values are `Option[X]` with the consumer branching semantically. Internal-mechanism constants (retry cadence, layer-init probe budgets) stay in code — they aren't "config". Pattern: [`config.md`](patterns/config.md).

---

## `db-lib` — the schema is one schema; `lib/db` owns it

The deployment has one MySQL schema, applied as one Flyway migration set, and consumed by every ctx that needs a repo. `lib/db` is the module that owns it: migrations, PEs (`<Entity>PE`), DbSchema traits, `SqlContext`, `Transactor`, `DataSourceLayer`, `NewTypeEncodings`. Ctxes depend on `libDb` and import PEs by name; a foreign-table read is an ordinary library import. Pattern: [`persistence.md`](patterns/persistence.md).

## `pe-converters` — PE ↔ domain mapping in dedicated converter objects

Mirrors `to-converters` for the persistence boundary. One `<Entity>PEConverter` object per PE in `<ctx>/impl/service/repo/converter/`. Methods named `to<Entity>` and `to<Entity>PE`. Pattern: [`persistence.md`](patterns/persistence.md).

## `tx-default` — repo methods open transactions; app services may wrap

Every repo method wraps its query in `Transactor.withTransaction`. App-service methods orchestrating multiple repo calls may wrap the orchestration; Quill's transaction is reentrant on a fiber-local connection, so nesting reuses the outer scope. Pattern: [`persistence.md`](patterns/persistence.md).

---

## `logging` — log levels by audience, ERROR at every boundary

INFO is for operators scanning a production log, DEBUG is for investigators troubleshooting a specific call, WARN is for recoverable failures that didn't surface, ERROR is for failures that did. Every boundary where a typed failure could be lost or converted gets an ERROR log, funneled through `LogError.tagged`. Duplicate logs across boundaries are fine; missing one is not. Pattern: [`logging.md`](patterns/logging.md).
