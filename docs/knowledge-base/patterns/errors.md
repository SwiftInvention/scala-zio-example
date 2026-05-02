# Errors

Typed errors flowing through the `Throwable` channel of `AppIO`. Trait signatures stay `AppIO[X]`; the value flowing on failure is a structured `AppFailure` subclass that the route boundary renders as `ErrorTO`.

## Layered structure

`lib/common/domain/error/`:

- `ErrorCategory.scala` — enumeratum of high-level categories (`Api`, `Backend`, `Customer`, …).
- `ErrorReason.scala` — empty marker trait. Per-category reason enums extend it.
- `AppFailure.scala` — abstract base. `extends Exception(message, cause)`, self-typed onto `HttpError`. Carries `category`, `reason`, `description`, `responseCode` (from the `HttpError` self-type).
- `api/HttpError.scala` — sealed trait + status-code traits (`HttpNotFound = 404`, `HttpBadRequest = 400`, `HttpInternalServerError = 500`, …). Concrete errors mix one in to declare their HTTP status.
- `api/ApiErrorReason.scala` + `api/ApiErrors.scala` — generic API errors (auth, validation, unhandled).
- `backend/BackendErrorReason.scala` + `backend/BackendErrors.scala` — generic backend errors (DB, internal).
- `api/ErrorTO.scala` — wire format `(code, category, reason, description)` + `from(e: AppFailure)` + zio-json codec.

Per-context (e.g. `customer/domain/error/`):

- `<Cat>ErrorReason.scala` — sealed trait extending `EnumEntry with ErrorReason`. Cases: `NotFound`, etc.
- `<Cat>Errors.scala` — abstract `<Cat>Error` base + concrete `final case class FooError(...)` mixing in an `Http*` trait.

## Concrete error shape

```scala
abstract class CustomerError(
    errorReason: CustomerErrorReason,
    message: String,
    cause: Option[Throwable] = None
) extends AppFailure(message, cause) { self: HttpError =>
  val category: ErrorCategory     = ErrorCategory.Customer
  val reason: CustomerErrorReason = errorReason
}

final case class CustomerNotFoundError private (message: String, cause: Option[Throwable] = None)
    extends CustomerError(NotFound, message, cause)
    with HttpNotFound

object CustomerNotFoundError {
  def withId(id: CustomerId): CustomerNotFoundError =
    CustomerNotFoundError(s"Customer with id=${CustomerId.unwrap(id)} is not found")
}
```

The companion smart constructor (`withId`) keeps message wording consistent at every call site.

## Failing with a typed error

`AppIO[T] = IO[Throwable, T]`. Concrete errors are `Throwable` subclasses, so they flow through the same channel:

```scala
override def get(id: CustomerId): AppIO[Customer] =
  repo.find(id).someOrFail(CustomerNotFoundError.withId(id))
```

`.someOrFail(e)` on `IO[E, Option[A]]` returns `IO[E, A]`, failing with `e` when None.

## Rendering at the boundary

Route handlers `mapError` to a `Response`. `AppFailure` becomes a structured 4xx/5xx with `ErrorTO` body; unknown `Throwable` gets wrapped in `UnhandledApiError` (500) so the wire format is uniform:

```scala
private def toErrorResponse(e: Throwable): Response = e match {
  case f: AppFailure => renderAppFailure(f)
  case other         => renderAppFailure(UnhandledApiError(other.getMessage, Some(other)))
}

private def renderAppFailure(f: AppFailure): Response =
  Response
    .json(ErrorTO.from(f).toJson)
    .status(Status.fromInt(f.responseCode))
```

## Why not typed error channels in the trait signatures

The alternative is `IO[<UnionOfErrors>, T]` with each method declaring its specific error variants. Pro: compile-time exhaustiveness over a method's failure set.

Con on Scala 2: the failure set is hand-maintained. Each layer either propagates the union manually (which requires Scala 3 union types to be ergonomic), or unifies to a wider `AppFailure` (loses information), or wraps in an ADT per layer (boilerplate).

We pick the `Throwable`-channel approach as a compromise: structure lives in the value, not the type. Routes pattern-match on `AppFailure` once at the boundary. Scala 3 union types would let us reintroduce a typed channel with the union built up automatically; until then, this shape.
