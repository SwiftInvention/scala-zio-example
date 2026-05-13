package com.example.lib.common.domain.error

/** Base class for typed application errors. `AppFailure` is the error type of `AppIO`: every failure in the managed
  * effect channel is one of its subclasses. JVM exceptions from JDBC, config parsing, etc. enter the channel only via
  * explicit `mapError` at the boundary — there is no implicit Throwable leakage.
  *
  * Extends `Exception` for cause chaining (we hold raw causes in `cause: Option[Throwable]` so stack traces and root
  * causes survive). Carries structured info — `category`, `reason`, `description` — that the route boundary reads to
  * render a wire response.
  *
  * Concrete errors mix in one of the `Http*` traits from `HttpError` to declare their status code.
  */
abstract class AppFailure(message: String, cause: Option[Throwable]) extends Exception(message, cause.orNull) {
  self: HttpError =>
  val category: ErrorCategory
  val reason: ErrorReason
  val description: String = message
  def responseCode: Int   = errorCode

  /** View this `AppFailure` as its `HttpError` mixin. The self-type guarantees every concrete subclass mixes in one of
    * `HttpError`'s sealed variants, so this widening is total — and matches on the result are compile-time exhaustive
    * over `HttpError`'s sealed sub-traits.
    */
  def asHttpError: HttpError = this
}
