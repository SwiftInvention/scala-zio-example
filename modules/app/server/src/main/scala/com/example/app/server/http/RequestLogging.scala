package com.example.app.server.http

import java.nio.charset.StandardCharsets
import java.util.UUID

import zio._
import zio.http._

/** App-level HTTP middleware that puts request context onto every log line.
  *
  * Two pieces:
  *
  *   - [[requestId]] — annotates a fresh `request_id` UUID for the duration of the handler. Propagates fiber-locally,
  *     so any `ZIO.log*` call inside the handler (or anything it calls) carries the same id.
  *   - [[accessLog]] — emits a single INFO line at request completion with `method`, `url`, `status`, `duration_ms`.
  *     Built on `Middleware.requestLogging`; covers success and failure paths.
  *
  * Apply at the composition root (`ServerApp`) so every route gets the middleware regardless of which context owns it.
  *
  * Chain as `routes @@ accessLog @@ requestId`. zio-http's `@@` is left-associative and stacks via `transform`, so the
  * **rightmost** operand becomes the **outermost** wrapper. Putting `requestId` rightmost means the annotation scope is
  * open when `accessLog` emits its line — without that ordering, the access line lacks `request_id`.
  */
object RequestLogging {

  /** Per-request annotation scope. The lambda runs once per request and produces the annotation set zio-http hands to
    * `ZIO.logAnnotate`. The UUID generation is a Java side effect; that's how zio-http's API is shaped (sync `Request
    * \=> Set[LogAnnotation]`).
    */
  val requestId: Middleware[Any] =
    Middleware.logAnnotate { (_: Request) =>
      Set(LogAnnotation(key = "request_id", value = UUID.randomUUID().toString))
    }

  /** Single access-log line per request. All parameters passed explicitly per the `no-default-args` rule (zio-http's
    * own definition has defaults; we override them all). Headers and bodies are intentionally omitted — the route +
    * status + duration is the operator-level summary.
    */
  val accessLog: HandlerAspect[Any, Unit] =
    Middleware.requestLogging(
      level = (_: Status) => LogLevel.Info,
      loggedRequestHeaders = Set.empty,
      loggedResponseHeaders = Set.empty,
      logRequestBody = false,
      logResponseBody = false,
      requestCharset = StandardCharsets.UTF_8,
      responseCharset = StandardCharsets.UTF_8
    )
}
