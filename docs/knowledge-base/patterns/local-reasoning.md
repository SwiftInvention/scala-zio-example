# Local Reasoning

What a piece of code does should be derivable from reading *that* code plus its *named* dependencies — without descending into the bodies of those dependencies. The named dependencies form the local frame. The discipline is that the frame is honest: types and signatures don't lie about what's inside.

This is the discipline that lets a codebase scale. Without it, every change requires understanding the whole system, and every reader's mental model diverges from every other's. With it, you can read a function and trust what it says.

## What enables local reasoning

**Types as contracts.** Signatures declare inputs, outputs, failure modes, and effects. `def find(id: CustomerId): AppIO[Option[Customer]]` is a complete description of what a caller can rely on: takes a typed id, returns an option of customer in the effect channel. Nothing else.

**No hidden state.** Values are values. No mutation through aliased references, no globals, no thread-locals. A `Customer` you hold today is the same `Customer` next millisecond — nobody else has a way to change it under you.

**Closed sets.** Sealed traits/enums let pattern matches be exhaustive. You can know all the cases without grep'ing for subclasses. Open inheritance is action-at-a-distance: a method might be overridden somewhere you can't see.

**Explicit dependencies.** What a function needs is in its signature or its required layer; nothing is pulled from ambient context. `Transactor` is a parameter or a `ZIO.service` call, not a static or a thread-local.

**Bounded effects.** Effects are in types. `AppIO[X]` declares "this might do work in the world"; pure functions don't lie. A method returning a plain value doesn't secretly hit the network.

## At every level of abstraction

The discipline applies at every scale, with the unit of "code" widening:

- **Expression.** No surprise side effects in what looks like an arithmetic line. `x + y` doesn't write to a file.
- **Function.** Body respects the signature. No `throw` from a non-effectful return type. No I/O from a function that returns a pure value.
- **Class / module.** Public API is the surface; private state is invisible to callers. Reading the public methods + their docs is enough to use the class.
- **Bounded context.** Cross-context calls go through `<ctx>-api`, never internals. The customer ctx's domain models are not the order ctx's concern.
- **System.** Deployment-unit behavior is derivable from the composition root. `ServerEnv.scala` is the truth about what's wired; you don't have to grep for layer constructions.

The unit changes; the rule doesn't. At each level, what you need to understand the level is *at* the level (or in declared dependencies of the level), not somewhere you have to descend to find.

## Where this is enforced in the codebase

Most existing principles are mechanisms for preserving local reasoning at one level or another:

- **`bounded-context`, `ctx-api`, `import-direction`, `build-deps`** — module/context boundaries, build-graph-enforced
- **`errors`** — failure modes visible at signatures (with one caveat below)
- **`newtypes`, `smart-constructors`** — types carry validated facts; callers don't re-derive them
- **`config-shape`** — config is a typed value flowing through the system; nobody reads `Config` ambiently
- **`tx-default`** — the transaction scope of a method is readable at the method
- **`pe-layout`** — PEs don't leak; repo signatures are domain types

When you find yourself adding action-at-a-distance, one of these is being violated.

## Where the codebase weakens local reasoning

Honest acknowledgements — these are real costs we pay, deliberately or not:

**The `Throwable` channel.** `AppIO[A] = IO[Throwable, A]` doesn't tell you what failures are possible. To know what a method can fail with, you have to read the body. We chose this in `errors` (Scala 2 union types aren't ergonomic) — the cost is that exhaustive failure handling at the boundary is informal rather than compiler-checked.

**Trait contracts not fully captured by types.** Some of our traits have semantics (laws, ordering, what counts as nesting) that types don't express. `Transactor.withTransaction[A](io: AppIO[A]): AppIO[A]` doesn't tell you that nested calls reuse the outer connection — that's documented but not encoded. A reader has to descend or know the convention. The fix when it matters: tighten the docstring to be the contract; or richer types that encode more of the law.

**Implicit resolution.** Quill encodings, zio-json codecs, PureConfig readers — implicits get pulled into scope from elsewhere. The use site doesn't show what's resolved. Mostly fine because the resolutions are predictable, but a reader debugging "why does this not compile?" has to know the rules of implicit search, which are global.

**Macro-generated code.** Quill's `quote { ... }` generates SQL at compile time. What runs at runtime is the generated query, not the Scala source. Reading the source isn't quite the truth — though for the queries we write, the gap is small.

These don't invalidate the principle; they're trade-offs we made for other reasons (Scala 2 ergonomics, library choices). Naming them lets us see the cost.

## Antipatterns

Things that destroy local reasoning, listed so they're recognizable:

- **Open inheritance with overrides.** Method on a non-`final` class might be overridden in a subclass you don't see. Calling it doesn't tell you which body runs. Default to `final` for classes that aren't designed for extension.
- **Shared mutable state.** Globals, vars on objects, `ThreadLocal`. Anyone can change them; you can't reason about your view without grep'ing.
- **Mutation through aliased references.** Two consumers hold the same mutable collection; one mutates; the other's invariants break silently. Use immutable collections; treat case-class `copy` as the standard "modify."
- **Ambient context not declared.** Reading a class field that's a singleton on an outer object, or a Scala 2 implicit-context-style "current user" pulled from a `DynamicVariable`. The signature doesn't admit the dependency exists.
- **Stringly-typed conventions.** A `String` parameter that secretly means "must be valid JSON" or "must start with a slash" — the meaning lives in callers' heads, not in the type. Use a `Newtype` or a smart constructor.
- **Comments that route the reader elsewhere.** *"Mirrors `FileB`"*, *"per the article we read"*, *"as we decided in the meeting"* — the meaning is in another place the reader has to chase. External refs are unreachable; internal refs rot silently when one side evolves. Describe what the file does in its own terms.
- **Action at a distance via callbacks.** Listeners registered globally that fire from places the registration site can't trace. Avoid unless the dynamism is the point.
- **Reflection and dynamic dispatch beyond declared signatures.** `cast`, `instanceOf`, `Class.forName` — what runs is decided by data, not types.

## Local reasoning applies to docs, too

A doc that requires you to chase four links to understand its premise has bad local reasoning, same as a function that requires reading four others. The "respect the reader, lean prose" discipline in this knowledge base is the same property at the documentation level: a doc should let you understand what it's describing without descending into others, beyond a small set of named pointers.

## When the discipline bends

Some kinds of code legitimately need non-local mechanisms:

- Boot-time wiring (the composition root sees everything; that's its job)
- Cross-cutting infrastructure (logging, tracing — instrumentation by definition spans abstractions)
- Performance-critical paths where mutation buys real wins (rare; isolate behind interfaces)
- Pluggable extensibility where the dynamism is the feature

The judgment: is this code's job to be local-reasonable, or is its job to be the seam where non-local concerns are organized? Composition roots are exempt from local reasoning because they *are* the place where the global view lives. That's their value.
