package com.example.common.domain.error

import com.example.common.domain.error.api.HttpError

/** Base class for typed application errors.
  *
  * Extends `Exception` so concrete errors flow through the `Throwable` channel of
  * `AppIO` without changing trait signatures. Carries structured info — `category`,
  * `reason`, `description` — that the route boundary reads to render a wire response.
  *
  * Concrete errors mix in one of the `Http*` traits from `error.api.HttpError` to
  * declare their status code.
  */
abstract class AppFailure(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull) { self: HttpError =>
  val category: ErrorCategory
  val reason: ErrorReason
  val description: String = message
  def responseCode: Int   = errorCode
}
