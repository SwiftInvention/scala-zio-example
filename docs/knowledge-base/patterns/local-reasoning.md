# Local Reasoning

What a piece of code does should be derivable from reading *that* code plus its *named* dependencies — without descending into the bodies of those dependencies. The named dependencies form the local frame. The discipline is that the frame is honest: types and signatures don't lie about what's inside.

## What enables local reasoning

**Types as contracts.** Signatures declare inputs, outputs, failure modes, and effects. `def find(id: CustomerId): AppIO[Option[Customer]]` is a complete description of what a caller can rely on: takes a typed id, returns an option of customer in the effect channel. Nothing else.

**No hidden state.** Values are values. No mutation through aliased references, no globals, no thread-locals. A `Customer` you hold today is the same `Customer` next millisecond — nobody else has a way to change it under you.

**Closed sets.** Sealed traits/enums let pattern matches be exhaustive. You can know all the cases without grep'ing for subclasses.

**Explicit dependencies.** What a function needs is in its signature or its required layer; nothing is pulled from ambient context. `Transactor` is a parameter or a `ZIO.service` call, not a static or a thread-local.

**Bounded effects.** Effects are in types. `AppIO[X]` declares "this might do work in the world". A method returning a plain value doesn't secretly hit the network.

## At every level of abstraction

The discipline applies at every scale, with the unit of "code" widening:

- **Expression.** No surprise side effects in what looks like an arithmetic line. `x + y` doesn't write to a file.
- **Function.** Body respects the signature. No `throw` from a non-effectful return type. No I/O from a function that returns a pure value.
- **Class / module.** Public API is the surface; private state is invisible to callers. Reading the public methods + their docs is enough to use the class.
- **Bounded context.** Cross-context calls go through `<ctx>-api`, never internals. The customer ctx's domain models are not the order ctx's concern.
- **System.** Deployment-unit behavior is derivable from the composition root. `ServerEnv.scala` is the truth about what's wired; you don't have to grep for layer constructions.

The unit changes; the rule doesn't.

## Where this is enforced in the codebase

Most existing principles are mechanisms for preserving local reasoning at one level or another:

- **`bounded-context`, `ctx-api`, `import-direction`, `build-deps`** — module/context boundaries, build-graph-enforced
- **`errors`** — failure modes visible at signatures
- **`newtypes`, `smart-constructors`** — types carry validated facts; callers don't re-derive them
- **`config-shape`** — config is a typed value flowing through the system; nobody reads `Config` ambiently
- **`tx-default`** — the transaction scope of a method is readable at the method
- **`db-lib`** — PEs live in the shared `lib/db`; ctx repo signatures are domain types

## Where the codebase weakens local reasoning

**Per-method failure precision.** `AppIO[A] = IO[AppFailure, A]` tells you the failure is one of our structured errors, not which subclass. To know whether `CustomerService.get` can fail with `CustomerNotFoundError` vs `DbError`, you read the body. The trade-off and its Scala 3 successor live in [`errors.md`](errors.md) §"Why not per-method failure types".

**Trait contracts not fully captured by types.** Some of our traits have semantics (laws, ordering, what counts as nesting) that types don't express. `Transactor.withTransaction[A](io: AppIO[A]): AppIO[A]` doesn't tell you that nested calls reuse the outer connection — that's documented but not encoded. A reader has to descend or know the convention.

**Implicit resolution.** Quill encodings, zio-json codecs, PureConfig readers — implicits get pulled into scope from elsewhere. The use site doesn't show what's resolved. A reader debugging "why does this not compile?" has to know the rules of implicit search, which are global.

**Macro-generated code.** Quill's `quote { ... }` generates SQL at compile time. What runs at runtime is the generated query, not the Scala source. Reading the source isn't quite the truth.

## Antipatterns

Things that destroy local reasoning:

- **Open inheritance with overrides.** Method on a non-`final` class might be overridden in a subclass you don't see. Calling it doesn't tell you which body runs. Default to `final` for classes that aren't designed for extension.
- **Shared mutable state.** Globals, vars on objects, `ThreadLocal`. Anyone can change them; you can't reason about your view without grep'ing.
- **Mutation through aliased references.** Two consumers hold the same mutable collection; one mutates; the other's invariants break silently. Use immutable collections; treat case-class `copy` as the standard "modify."
- **Ambient context not declared.** Reading a class field that's a singleton on an outer object, or a Scala 2 implicit-context-style "current user" pulled from a `DynamicVariable`. The signature doesn't admit the dependency exists.
- **Stringly-typed conventions.** A `String` parameter that secretly means "must be valid JSON" or "must start with a slash" — the meaning lives in callers' heads, not in the type. Use a `Newtype` or a smart constructor.
- **Comments that route the reader elsewhere.** *"Mirrors `FileB`"*, *"per the article we read"*, *"as we decided in the meeting"* — the meaning is in another place the reader has to chase. External refs are unreachable; internal refs rot silently when one side evolves. Describe what the file does in its own terms.
- **Action at a distance via callbacks.** Listeners registered globally that fire from places the registration site can't trace. Avoid unless the dynamism is the point.
- **Reflection and dynamic dispatch beyond declared signatures.** `cast`, `instanceOf`, `Class.forName` — what runs is decided by data, not types.

## Local reasoning applies to docs, too

A doc that requires you to chase four links to understand its premise has bad local reasoning, same as a function that requires reading four others. A doc should be self-contained beyond a small set of named pointers.

See [`prose.md`](prose.md) for the full prose standard and the antipatterns that violate it.

## When the discipline bends

Some kinds of code legitimately need non-local mechanisms:

- Composition roots — see [`composition-roots.md`](composition-roots.md).
- Cross-cutting infrastructure (logging, tracing — instrumentation by definition spans abstractions).
- Performance-critical paths where mutation buys real wins — isolate behind interfaces.
- Pluggable extensibility where the dynamism is the feature.
