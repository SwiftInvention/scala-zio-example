# Newtypes

Wrapped primitives carrying semantic distinction at the type level. Used for domain ids and constrained value objects.

## Why

Two bugs newtypes close:

1. **Argument-swap silence.** `findCustomer(orderId)` compiles when both ids are `String`. With newtypes, the swap is a compile error.
2. **Invalid values entering the domain.** `String` accepts anything; `Email("not-an-email")` should fail. Smart-constructed newtypes fail at construction.

Wire shape stays flat: `JsonCodec[U].transform(apply, unwrap)` serializes the wrapped value as the underlying primitive — no `{"id": {"value": "c-001"}}` wrapping on the wire.

## Library

zio-prelude `Newtype`. The `io.estatico/newtype` dep listed in `Versions.scala` is unused — kept on the deps list as a marker, slated for removal. Scala 3 opaque types are the natural successor; we'll switch when we cross the 2 → 3 line.

## Shape

```scala
import zio.json.JsonCodec
import zio.prelude.Newtype

object CustomerId extends Newtype[String] {
  implicit val codec: JsonCodec[Type] =
    JsonCodec[String].transform(apply, unwrap)
}
type CustomerId = CustomerId.Type
```

The `type CustomerId = CustomerId.Type` alias lets call sites write `CustomerId` directly. `apply`/`unwrap` are the constructor and escape hatch. The wrapped type is opaque — String methods aren't available without `unwrap`, which is the point.

## Naming

- Ids: `<Entity>Id` (`CustomerId`, `OrderId`)
- Value objects: the natural type name (`Email`, `Phone`, `Url`)

## Where they live

**Ids — centralized** in `lib/common/.../domain/model/NewTypes.scala`. Ids have no validation logic (a `CustomerId` is whatever the database calls it); centralizing keeps them out of every ctx's import list and makes the Quill-encoding setup trivially co-located.

**Value objects — per-ctx**, in `<ctx>/domain/model/<TypeName>.scala`. Their validation rules are domain-specific. Putting them next to the ctx's other domain models keeps the rules visible to readers exploring that ctx.

**Cross-cutting value types** (used by multiple ctxs): `lib/common/.../domain/model/shared/`. Plumcheck does this with `Email` and `Phone`. We don't have that case yet but it's the valid escape hatch.

## Smart constructors

For validated value objects, override the construction path so invalid input fails. The exact zio-prelude API is version-sensitive; we'll pin the canonical shape when we add the first validated newtype to this template. Sketch:

```scala
object Email extends Newtype[String] {
  // shape depends on the prelude API in use
  // — overrides apply (or assertion) to enforce a regex
  // — runtime construction returns Validation[String, Email]
}
```

When a converter (TO → domain, PE → domain) calls a validating constructor and the input is invalid, the failure surfaces as an `AppFailure` — typically a validation-category error rendered at the route boundary as 400.

## Quill `MappedEncoding`

Quill needs `MappedEncoding[NT, U]` and `MappedEncoding[U, NT]` to read/write a newtype-typed column.

**Centralized ids** — encodings live in `lib/common/.../impl/repo/pg/NewTypeEncodings.scala` and are mixed into `PgContext`:

```scala
trait NewTypeEncodings {
  implicit val customerIdEncoding: MappedEncoding[CustomerId, String] =
    MappedEncoding[CustomerId, String](id => CustomerId.unwrap(id))
  implicit val customerIdDecoding: MappedEncoding[String, CustomerId] =
    MappedEncoding[String, CustomerId](s => CustomerId(s))
}
```

Adding a new id: two lines here, no other module touches.

**Per-ctx value objects** — encodings live in the ctx's `<Name>DbSchema` trait alongside the table mapping:

```scala
trait CustomerDbSchema {
  val ctx: PgContext
  import ctx._

  implicit val emailEncoding: MappedEncoding[Email, String] = ...
  implicit val emailDecoding: MappedEncoding[String, Email] = ...

  protected val customerTable = quote(querySchema[CustomerPE]("customer"))
}
```

Co-locates the encoding with the type and with the schema that uses it.

## When to use, when to skip

**Use newtypes for:**

- Identifiers (always)
- Constrained primitives (`Email`, `PositiveInt`, `ISBN`)
- Primitives whose semantic type is genuinely different from peers nearby — a `FirstName` next to a `LastName` next to a `MiddleName`, even without validation, because the swap is a real concern

**Skip for:**

- Transient internal labels with no semantic identity (a one-shot `description: String`)
- Strings that have a name but no domain identity (`note`, `comment`)

The middle case — "should I newtype this `Name`?" — is judgment. Plumcheck wraps almost everything semantic; we lean lighter. When in doubt: ids yes, things with validation yes, things that are just named strings probably no.
