package com.example.lib.common.impl.http.server.middleware

import io.opentelemetry.api.trace.{SpanKind, StatusCode}
import zio._
import zio.http._
import zio.telemetry.opentelemetry.tracing.{StatusMapper, Tracing}

/** Server-span middleware: opens one span per HTTP request, named `<METHOD> <path>`, kind `SERVER`. The handler runs
  * inside the span scope; the span closes on response (success or failure).
  *
  * Hand-rolled because zio-telemetry's `tracing.aspects.span` is a `ZIOAspect`, not a zio-http `Middleware[Tracing]`.
  * The middleware adds `Tracing` to the env requirement of the routes it's applied to; the composition root supplies
  * it.
  *
  * Span status follows HTTP semantics: 4xx ends the span with `OK` (the server responded correctly to a bad request);
  * 5xx ends with `ERROR`. zio-http's `Routes[R, Response]` puts both 4xx and 5xx in the failure channel, so without
  * this mapper a 404 would be tagged as a span error.
  *
  * No trace-context extraction from inbound headers — every request opens a fresh root span. Add
  * `tracing.extractSpan(...)` here if/when the system needs to chain under an upstream caller.
  */
object RequestTracing {

  /** Status mapper: 5xx response → `ERROR`, anything else → `OK`. */
  private val httpStatusMapper: StatusMapper[Response, Response] =
    StatusMapper.both(
      failure = StatusMapper.failureNoException[Response] { resp =>
        if (resp.status.code >= 500) StatusCode.ERROR else StatusCode.OK
      },
      success = StatusMapper.successNoDescription[Response] { resp =>
        if (resp.status.code >= 500) StatusCode.ERROR else StatusCode.OK
      }
    )

  val span: Middleware[Tracing] = new Middleware[Tracing] {
    override def apply[Env <: Tracing, Err](routes: Routes[Env, Err]): Routes[Env, Err] =
      routes.transform[Env] { h =>
        Handler.fromFunctionZIO[Request] { req =>
          ZIO.scoped[Env] {
            ZIO.serviceWithZIO[Tracing] { tracing =>
              tracing.span(
                spanName = s"${req.method} ${req.path}",
                spanKind = SpanKind.SERVER,
                statusMapper = httpStatusMapper
              )(h.runZIO(req))
            }
          }
        }
      }
  }
}
