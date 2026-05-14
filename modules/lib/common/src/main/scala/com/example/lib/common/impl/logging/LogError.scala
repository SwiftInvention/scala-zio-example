package com.example.lib.common.impl.logging

import com.example.lib.common.domain.error.AppFailure
import zio._

/** Funnel for `AppFailure` error logging at boundaries.
  *
  * Use `.tapError(LogError.tagged("Context.name"))` at every boundary where the typed error could be lost or converted
  * to a different shape:
  *
  *   - HTTP route handlers, before `mapError(renderAppFailure)` — the log keeps the typed `category`, `reason`, and
  *     status code as queryable annotations; the response gets the converted `ErrorTO`.
  *   - Catchall handlers, message handlers, RPC handlers — anywhere `mapError` / `catchAll` converts the error.
  *   - Forked fibers (`forkDaemon`, `forkScoped`) — the forked effect must catch and log its own failures; the parent's
  *     `tapError` doesn't see them.
  *   - The app's top-level entrypoint — last-resort log before the runtime swallows the cause.
  *
  * Duplicate logs across boundaries are fine; missing one is not.
  *
  * `context` is a free-form label that identifies the boundary in the log (e.g. `"CustomerRoutes.get"`,
  * `"BillMatchingFiber"`). Goes at the front of the message.
  */
object LogError {

  /** Returns a function suitable for `tapError`. Logs the failure as an `ERROR` with `Cause.fail(failure)` (full stack
    * preserved) and annotates `error.category`, `error.reason`, `error.status_code`, `error.message`.
    *
    * Headline uses `failure.description` (the wire-safe summary; `BackendError` subclasses collapse this to `"Internal
    * server error"` so the rendered HTTP response doesn't leak internals). `error.message` carries `failure.getMessage`
    * so the diagnostic detail stays queryable as a flat structured field.
    */
  def tagged(context: String)(failure: AppFailure): UIO[Unit] = {
    val annotations = Set(
      LogAnnotation(key = "error.category", value = failure.category.entryName),
      LogAnnotation(key = "error.reason", value = failure.reason.toString),
      LogAnnotation(key = "error.status_code", value = failure.responseCode.toString),
      LogAnnotation(key = "error.message", value = failure.getMessage)
    )
    ZIO.logAnnotate(annotations) {
      ZIO.logErrorCause(s"$context: ${failure.description}", Cause.fail(failure))
    }
  }
}
