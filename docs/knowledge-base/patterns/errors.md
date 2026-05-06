# Errors

Every domain operation can fail. We want failures to be precise (the route boundary needs to know whether to return 404 or 500) and ergonomic (no method in any layer should have to enumerate its failure set in its return type). The compromise we land on: use ZIO's `Throwable` channel, but make the values flowing through it carry structure.

A failure is an `AppFailure` — a `Throwable` subclass that knows its HTTP status, its category, and its reason. Trait signatures stay `AppIO[X]` (i.e. `IO[Throwable, X]`). At the route boundary, we pattern-match on `AppFailure` once and render it as `ErrorTO` on the wire.

## Anatomy of an error

Errors are organized along three axes:

- **Category** — broad, like `Api`, `Backend`, `Customer`. Groups failures by which part of the system raised them.
- **Reason** — fine-grained within a category, like `NotFound` or `AlreadyExists`. Modeled per-category as an enumeratum sealed enum.
- **HTTP status** — mixed in via a marker trait (`HttpNotFound = 404`, `HttpBadRequest = 400`, …). The status is part of the type, not a runtime property.

A concrete error pulls all three together:

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

The `withId` companion method is a smart constructor — it keeps the message wording consistent across every site that raises this error.

## Where the pieces live

Concrete context-specific errors and their reason enums live with the context that raises them: `<ctx>/domain/error/` holds `<Cat>ErrorReason.scala` and `<Cat>Errors.scala`. Generic infrastructure — the `AppFailure` base, the `HttpError` status traits, the wire-format `ErrorTO`, and pre-built generic errors (`ApiErrors` for auth/validation/unhandled, `BackendErrors` for DB/internal) — lives in `lib/common/domain/error/`. So `CustomerNotFoundError` ships with the customer context, but the `HttpNotFound` trait it mixes in comes from `lib/common`. Before defining a new error, check whether one of the generic ones already says what you need.

## Failing with a typed error

`AppFailure` is a `Throwable`, so you fail with one the same way you'd fail with any throwable:

```scala
override def get(id: CustomerId): AppIO[Customer] =
  repo.find(id).someOrFail(CustomerNotFoundError.withId(id))
```

(`someOrFail` on `IO[E, Option[A]]` returns `IO[E, A]`, failing with the given value when the option is empty.)

## Rendering at the boundary

Route handlers `mapError` once, turning anything raised during request handling into an HTTP response. An `AppFailure` becomes a structured 4xx/5xx with an `ErrorTO` body. Anything else — any `Throwable` that isn't an `AppFailure` — gets wrapped in `UnhandledApiError` (500), so the wire format stays uniform and a stray exception never leaks through raw.

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

## Why not put the failure types in the signature

The obvious alternative is what ZIO is built for: `IO[CustomerNotFoundError | InvalidEmailError, Customer]`, with the failure set declared in the type and exhaustiveness checked at compile time. We'd take that in a heartbeat — except we're on Scala 2.

Without union types, the failure set in each signature has to be hand-maintained. Every layer either propagates a manually-spelled union (tedious), widens to a supertype like `AppFailure` (throwing away the precision we wanted), or wraps in a per-layer ADT (boilerplate that grows with every new error). None of those are good enough to justify the cost.

So the `Throwable`-channel approach: structure lives in the *value*, not the type. Routes pattern-match on `AppFailure` once at the boundary; layers below treat errors like any other effect failure. When we move to Scala 3, union types make a typed channel ergonomic enough to be worth revisiting.

The cost we pay today: a method's signature doesn't tell you which errors it can raise — you have to read the body. Local reasoning takes a hit; we name that openly in [`local-reasoning.md`](local-reasoning.md).
