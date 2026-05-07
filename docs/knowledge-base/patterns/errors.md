# Errors

Every domain operation can fail. We want failures to be precise (the route boundary needs to know whether to return 404 or 500) and the channel to be honest (no method should be able to fail with "anything Throwable" by accident). The shape we land on: a typed family in the error channel.

`AppIO[A]` is `IO[AppFailure, A]`. The error type isn't `Throwable` — it's `AppFailure`, the abstract base of every structured error we define. (`AppFailure` isn't `sealed` — concrete errors live across modules — but the convention is rigid: every error in the codebase extends it.) Every failure flowing through the channel is one of ours: it knows its HTTP status, its category, and its reason. Raw JVM/JDBC exceptions don't enter the channel implicitly — they're wrapped at the boundary via explicit `mapError`.

## What this buys

The win is small but real, and worth naming precisely:

- **No implicit Throwable leakage.** A repo method can't accidentally surface a `NullPointerException` from a Quill bug or a `SQLException` from the JDBC driver. The signature `AppIO[Customer]` is enforced: anything else has to be wrapped at the boundary, and the wrapping is grep-able.
- **The route boundary is exhaustive by construction.** `mapError` in a route receives `AppFailure` directly, not `Throwable`. There's no "something we forgot about" case to handle — the type system covers it.
- **All errors are defined by us.** When you read the codebase, every failure mode is a class somewhere in our `error/` folders. There's no third-party error type bleeding through.

What this *doesn't* buy: per-method failure precision. The channel widens to `AppFailure` everywhere, so a method's signature doesn't tell you "this can fail with `CustomerNotFoundError` specifically." That's a Scala-2 limitation we accept — see [Why not per-method failure types](#why-not-per-method-failure-types) below.

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

`AppFailure` extends `Exception` for cause chaining (we hold a raw `Option[Throwable]` cause so stack traces survive when something *did* originate from a JVM exception). The `Exception`-ness is internal plumbing — the channel type is `AppFailure`, and that's what every consumer sees.

## Where the pieces live

Concrete context-specific errors and their reason enums live with the context that raises them: `<ctx>/domain/error/` holds `<Cat>ErrorReason.scala` and `<Cat>Errors.scala`. Generic infrastructure — the `AppFailure` base, the `HttpError` status traits, the wire-format `ErrorTO`, and pre-built generic errors (`ApiErrors` for auth/validation/unhandled, `BackendErrors` for DB/internal/config) — lives in `lib/common/domain/error/`. So `CustomerNotFoundError` ships with the customer context, but the `HttpNotFound` trait it mixes in comes from `lib/common`. Before defining a new error, check whether one of the generic ones already says what you need.

## Failing with a typed error

Failure in the channel is just `ZIO.fail` (or `someOrFail`, etc.):

```scala
override def get(id: CustomerId): AppIO[Customer] =
  repo.find(id).someOrFail(CustomerNotFoundError.withId(id))
```

(`someOrFail` on `IO[E, Option[A]]` returns `IO[E, A]`, failing with the given value when the option is empty.)

## Wrapping raw Throwables at boundaries

JVM/JDBC/library code surfaces `Throwable`s. We let those into the `AppFailure` channel only via explicit `mapError`:

```scala
// PgContext.runQuery — Quill produces Throwable, we wrap as DbError
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

The pattern: at every place where a `Throwable`-channel effect meets our managed channel, write the wrapping inline. The boundaries are few (datasource, query execution, transaction, config load) and easy to enumerate.

## Rendering at the boundary

Route handlers `mapError` once, turning the typed failure into an HTTP response:

```scala
private def renderAppFailure(f: AppFailure): Response =
  Response
    .json(ErrorTO.from(f).toJson)
    .status(Status.fromInt(f.responseCode))
```

The handler receives `AppFailure` directly — no pattern match against `Throwable`, no fallback for "something else." The compiler guarantees there is nothing else.

`UnhandledApiError` is a 500-status `ApiError` for unexpected conditions at boundaries — wrap a surprising `Throwable` here when none of the specific errors fit. It's not used at the route level: by the time the route's `mapError` runs, the channel has already narrowed to `AppFailure`.

## Why not per-method failure types

The next step up in precision is what ZIO is built for: `IO[CustomerNotFoundError | InvalidEmailError, Customer]`, with the failure set declared in the type and exhaustiveness checked at compile time. We'd take that — except we're on Scala 2.

Without union types, a per-method failure set has to be hand-maintained. Every layer either propagates a manually-spelled union (tedious), widens to the family supertype (which is exactly what `IO[AppFailure, A]` already is), or wraps in a per-layer ADT (boilerplate that grows with every new error). The first and third cost too much; the second is what we ship. So the family-level bound is the most precise thing reachable in Scala 2 without per-call-site overhead, and it's what we have.

The cost we pay: a method's signature tells you "the failure is one of our structured errors" but not "this specific subset." For exhaustive case-handling on errors raised by a specific method, the reader has to consult the body. Local reasoning takes a hit at that level — bounded by the `AppFailure` family rather than scoped to the method.
