# Newtypes

A `String` is a `String`, but a `CustomerId` and an `OrderId` shouldn't be interchangeable. A newtype gives a primitive a distinct type at compile time without changing what flows on the wire.

This doc covers the zio-prelude `Newtype` mechanism — used for **ids**, which don't carry validation. For value objects with validation (`Email`, `Phone`), see [`smart-constructors.md`](smart-constructors.md); that uses a different mechanism (`sealed abstract case class`) for a different job.

## Why

`findCustomer(orderId)` compiles when both ids are plain `String`. With newtypes, the swap is a compile error. Discipline and comments don't catch this; the compiler does.

Wire shape stays flat: `JsonCodec[U].transform(apply, unwrap)` serializes the wrapped value as the underlying primitive — no `{"id": {"value": "c-001"}}` wrapping object on the wire.

## Library

zio-prelude `Newtype`. Scala 3 opaque types are the natural successor; we'll switch when we cross the 2 → 3 line.

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

All `<Entity>Id`s live in `lib/common/.../domain/model/NewTypes.scala` — centralized. Quill `MappedEncoding`s for them live with the rest of the persistence infrastructure in `lib/db/.../impl/repo/sql/NewTypeEncodings.scala`.

Centralizing makes sense for ids specifically because they have no validation logic (a `CustomerId` is whatever the database calls it), and one home for both the type and its encoding keeps "adding a new id" a two-line change. Validated value objects don't centralize the same way — see [`smart-constructors.md`](smart-constructors.md).

## Quill `MappedEncoding`

Quill needs `MappedEncoding[NT, U]` and `MappedEncoding[U, NT]` to read/write a newtype-typed column. For centralized ids, encodings live in `lib/db/.../impl/repo/sql/NewTypeEncodings.scala` and are mixed into `SqlContext`:

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
