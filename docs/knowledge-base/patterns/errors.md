# Errors

`AppIO[A]` is `IO[AppFailure, A]`. The error type isn't `Throwable` — it's `AppFailure`, the abstract base of every structured error we define. Concrete errors live across modules, so `AppFailure` itself isn't `sealed`; the convention is rigid by code review. Every failure in the channel is one of ours: it knows its HTTP status, its category, and its reason. Raw JVM/JDBC exceptions enter the channel only via explicit `mapError` at the boundary.

## What this buys

- **No implicit Throwable leakage.** A repo method can't accidentally surface a `NullPointerException` or `SQLException`. Anything not an `AppFailure` has to be wrapped at the boundary, and the wrapping is grep-able.
- **The route boundary is exhaustive by construction.** `mapError` in a route receives `AppFailure` directly. There's no "something we forgot about" case to handle.

What this doesn't buy: per-method failure precision. `IO[AppFailure, A]` widens at the family supertype, not the specific subclass — see [Why not per-method failure types](#why-not-per-method-failure-types).

## Anatomy of an error

Three axes:

- **Category** — broad, like `Api`, `Backend`, `Customer`.
- **Reason** — fine-grained within a category, like `NotFound` or `AlreadyExists`. Modeled per-category as an enumeratum sealed enum.
- **HTTP status** — mixed in via a marker trait (`HttpNotFound = 404`, `HttpBadRequest = 400`, …). Part of the type, not a runtime property.

Pulling all three together:

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

The `withId` smart constructor keeps the message wording consistent across raise sites.

`AppFailure` extends `Exception` for cause chaining — concrete errors hold a raw `Option[Throwable]` so JVM-origin stack traces survive. The `Exception`-ness is internal plumbing; the channel type is `AppFailure`.

## Where the pieces live

Context-specific errors and reason enums live with the context: `<ctx>/domain/error/` holds `<Cat>ErrorReason.scala` and `<Cat>Errors.scala`. Generic infrastructure — `AppFailure`, the `HttpError` status traits, `ErrorTO`, pre-built generic errors (`ApiErrors`, `BackendErrors`) — lives in `lib/common/domain/error/`. Check the generic ones before defining a new error.

## Failing with a typed error

```scala
override def get(id: CustomerId): AppIO[Customer] =
  repo.find(id).someOrFail(CustomerNotFoundError.withId(id))
```

`someOrFail` on `IO[E, Option[A]]` returns `IO[E, A]`, failing with the given value when the option is empty.

## Wrapping raw Throwables at boundaries

JVM/JDBC/library code surfaces `Throwable`s; explicit `mapError` is the only path into the channel:

```scala
// SqlContext.runQuery — Quill produces Throwable, we wrap as DbError
def runQuery[A](q: => QIO[A]): AppIO[A] =
  q.provideEnvironment(ZEnvironment(ds))
    .mapError(e => DbError(s"Database query failed: ${e.getMessage}", Some(e)))

// ConfigBootstrap.load — pureconfig and typesafe-config errors → ConfigError
ZIO.attempt(ConfigFactory.parseResources(resource).resolve())
  .mapError(e => ConfigError(s"Failed to parse '$resource': ${e.getMessage}", Some(e)))

// TransactorQuillImpl — Quill's transaction widens to Throwable; narrow back
ctx.transaction(io)
  .provideEnvironment(ZEnvironment(ctx.ds))
  .mapError {
    case f: AppFailure            => f
    case e: java.sql.SQLException => DbError(s"Transaction failed: ${e.getMessage}", Some(e))
    case other                    => InternalServerError(s"Unexpected: ${other.getMessage}", Some(other))
  }
```

The boundaries are few (datasource, query execution, transaction, config load) and enumerable.

## Rendering at the boundary

```scala
private def renderAppFailure(f: AppFailure): Response =
  Response
    .json(ErrorTO.from(f).toJson)
    .status(Status.fromInt(f.responseCode))
```

The handler receives `AppFailure` — no pattern match against `Throwable`, no fallback case.

`UnhandledApiError` is a 500-status `ApiError` for boundary wrapping when none of the specific errors fit. Not used at the route level: the channel has already narrowed by the time `mapError` runs there.

## Why not per-method failure types

The channel widens at `AppFailure`, not at the specific subclass, so signatures don't tell you "this method can fail with `CustomerNotFoundError`." Owner of this trade-off discussion: [`local-reasoning.md`](local-reasoning.md) §"Per-method failure precision".
