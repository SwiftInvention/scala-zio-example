# Newtypes

Wrapped primitives carrying semantic distinction at the type level. This doc covers the zio-prelude `Newtype` mechanism, used for **ids** — wrappers without validation. For value objects with validation rules (`Email`, `Phone`), see [`smart-constructors.md`](smart-constructors.md), which uses a different mechanism (`sealed abstract case class`).

## Why

The bug: `findCustomer(orderId)` compiles when both ids are `String`. With a newtype, the swap is a compile error. The compiler does the work that comments and discipline don't.

Wire shape stays flat: `JsonCodec[U].transform(apply, unwrap)` serializes the wrapped value as the underlying primitive — no `{"id": {"value": "c-001"}}` wrapping.

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

`<Entity>Id` for entity ids (`CustomerId`, `OrderId`).

## Where they live

All `<Entity>Id`s live in `lib/common/.../domain/model/NewTypes.scala` — centralized.

Centralizing makes sense for ids specifically because they have no validation logic (a `CustomerId` is whatever the database calls it), and centralization keeps Quill-encoding setup trivially co-located. Validated value objects don't centralize the same way — see [`smart-constructors.md`](smart-constructors.md).

## Quill `MappedEncoding`

Quill needs `MappedEncoding[NT, U]` and `MappedEncoding[U, NT]` to read/write a newtype-typed column. For centralized ids, encodings live in `lib/common/.../impl/repo/pg/NewTypeEncodings.scala` and are mixed into `PgContext`:

```scala
trait NewTypeEncodings {
  implicit val customerIdEncoding: MappedEncoding[CustomerId, String] =
    MappedEncoding[CustomerId, String](id => CustomerId.unwrap(id))
  implicit val customerIdDecoding: MappedEncoding[String, CustomerId] =
    MappedEncoding[String, CustomerId](s => CustomerId(s))
}
```

Adding a new id: two lines here, no other module touches.

## When to use, when to skip

**Use a newtype for:**

- Identifiers (always)
- Primitives whose semantic type is genuinely different from peers nearby — a `FirstName` next to a `LastName` next to a `MiddleName`, even without validation, because the swap is a real concern

**Skip for:**

- Transient internal labels with no semantic identity (a one-shot `description: String`)
- Strings that have a name but no domain identity (`note`, `comment`)

For values with **validation rules** (an email format, a positive integer), use the smart-constructor recipe instead — that's a different mechanism with the validation baked into the construction. See [`smart-constructors.md`](smart-constructors.md).

The middle case — "should I newtype this `Name`?" — is judgment. Plumcheck wraps almost everything semantic; we lean lighter. When in doubt: ids yes, things-with-validation yes (via smart constructors), things that are just named strings probably no.
