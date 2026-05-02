# Architecture Principles

Architectural commitments the codebase adheres to.

Same migration policy as `styleguide.md`: every existing violation is fixed, marked `FIXME` (intent to fix), or marked `WONTFIX` (with reason). New code introduces no unmarked violations.

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
