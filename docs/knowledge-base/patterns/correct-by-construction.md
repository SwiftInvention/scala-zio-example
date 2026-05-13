# Correct by Construction

Type discipline at the heart of the codebase: shape domain types so the compiler proves the invariants the runtime never has to. *Make illegal states unrepresentable.*

## ADTs — model exactly the legal states

Sum types (sealed traits, enums) and product types (case classes) express exactly which states the domain admits. No more, no less.

Don't model a user who's either authenticated or anonymous as a single `User` with a nullable `authToken: Option[String]`. Use a sum type:

```scala
sealed trait Visitor
final case class Authenticated(userId: UserId, token: AuthToken) extends Visitor
case object Anonymous extends Visitor
```

Code that needs to handle a `Visitor` pattern-matches on it; the compiler enforces exhaustiveness. Add a third variant later (`BotUser`) and every match site fails to compile until updated.

**Prefer functions on the sum, not on individual variants.** A function `def foo(a: Authenticated)` sits *outside* the exhaustiveness net — when a new variant lands, `foo` keeps compiling silently and you lose the structural prompt to consider whether it should serve the new case. Take the sum and dispatch internally via `match`. Take a variant directly only when the function structurally cannot serve any other variant. Variants are implementation details of the sum; the sum is the public surface.

## Smart constructors — enforce invariants at construction

If a value has invariants (an `Email` matches a format; a `PositiveInt` is positive), the constructor enforces them — and is the only place that can fail. The canonical Scala recipe is `sealed abstract case class Foo private (...)` with anonymous-subclass construction inside the companion. The naive `final case class Foo private (...)` leaks via the auto-generated `copy()`; the `abstract` keyword neutralizes that. Full recipe, gotchas, and the codec-boundary detail: [`smart-constructors.md`](smart-constructors.md).

## Parse, don't validate

At the boundary between untyped input (HTTP body, DB row, config file, env var) and the domain, **parse** — produce a typed value that captures the validation. Don't **validate-and-pass-through** — check that a `String` is well-formed, then keep passing the `String` with the validation knowledge implicit.

The boundary is the converter (`<Entity>Converter` for HTTP, `<Entity>PEConverter` for persistence, `XConfig` parsers for config). What flows out is typed and trusted; what flows in is untyped and skeptical.

The bug this kills: same value gets validated twice in some paths and zero times in others, and the type system can't tell which.

## Domain types stay at the domain

Smart-constructed values (`Email`, `CustomerName`, `PostalCode`) and other domain types stay at the domain layer. They don't propagate outward through the converter.

A `CustomerTO` carries `email: String`, not `email: Email` — the wire format has its own constraints (JSON serialization stability, schema evolution, the set of fields exposed vs. the set modeled in the domain) and isn't a thinner `Customer`. Same for `CustomerPE` on the persistence side: it matches the schema's column shape, which may diverge from the domain (denormalized fields, computed columns, audit metadata). The converters project domain values out and parse boundary values in; pulling domain types outward across either boundary creates conflicts the boundary's constraints can't accommodate.

Per-value exceptions to the outbound rule are deliberate. `CustomerId` (a `Newtype`) travels as a flat `String` over the wire via its codec but stays a typed value in Scala — worth it because typed ids prevent argument swaps at every call site that holds one. `Email`, by contrast, is internal-only: the smart-constructed type flattens to `String` outbound, since the wire receiver typically just renders it. The judgment is per-value: does the consumer at the boundary actually benefit from the type, or is the boundary just rendering / storing it?

## Correct by construction — the property you get

With ADTs, smart constructors, and parse-don't-validate in place, every function operating on a typed value trusts the type's invariants without re-checking.

You pay upfront with richer types and smarter boundaries. In return, invalid-state bugs become compile errors.

## Where this shows up

The `newtypes`, `smart-constructors`, `errors`, `config-shape`, `to-converters`, and `pe-converters` principles are all instances. Owner of the per-principle enumeration: [`architecture-principles.md`](../architecture-principles.md).

## Antipatterns we reject

**Validate-and-pass-through.** `def validate(s: String): Boolean; if (validate(s)) doSomething(s)` — the `s: String` keeps flowing, validation knowledge implicit. Parse: `def parse(s: String): Either[Err, T]; parse(s).foreach(doSomething)`.

**ADT-as-nullable-fields.** A class with mutually-exclusive `Option` fields (`authToken: Option[String], anonymousId: Option[String]`) — the type permits illegal states (both set, neither set). Express the alternation as a sum type.

**Boolean blindness.** `def createUser(name: String, isAdmin: Boolean, isActive: Boolean)` reads at the call site as `createUser("Ada", true, false)` — opaque. Replace with named values, a domain-specific enum, or split methods. The signature should make the meaning visible at the call site.

**Option blindness.** `Option[X]` where the absent case has rich semantics distinct from "no value": `Option[FeatureConfig]` where `None` means "feature disabled". The type lets consumers `getOrElse` or `foreach` and silently collapse the distinction. Express as a sum:

```scala
sealed trait FeatureSetting
case object Disabled extends FeatureSetting
final case class Enabled(cfg: FeatureConfig) extends FeatureSetting
```

The consumer has to branch.

## When the philosophy bends

Don't pay the type cost where it doesn't earn its keep:

- Transient internal labels with no semantic identity (a one-shot debug `description: String`)
- Pure infrastructure where domain invariants don't apply (logger names, file paths)
- Test fixtures, ad-hoc scripts

The judgment: does the type protect something domain-meaningful? If "is this string valid?" is a real question, it deserves a type. If it isn't, leave the string alone.
