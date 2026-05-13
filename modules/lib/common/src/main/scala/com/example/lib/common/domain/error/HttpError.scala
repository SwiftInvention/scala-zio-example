package com.example.lib.common.domain.error

/** Self-typed onto `AppFailure` to carry an HTTP status code with a typed error.
  *
  * Each concrete error mixes in one of the status traits below. The route boundary reads `errorCode` to set the
  * response status. `errorCode` is `final` on every status trait — a subtrait can't override the value (e.g. a
  * hand-rolled `HttpTeapot extends HttpBadRequest` can't shift the wire status away from 400).
  */
sealed trait HttpError {
  val errorCode: Int
}

trait HttpBadRequest extends HttpError {
  final val errorCode: Int = 400 // scalafix:ok DisableSyntax.noFinalVal
}

trait HttpUnauthorized extends HttpError {
  final val errorCode: Int = 401 // scalafix:ok DisableSyntax.noFinalVal
}

trait HttpForbidden extends HttpError {
  final val errorCode: Int = 403 // scalafix:ok DisableSyntax.noFinalVal
}

trait HttpNotFound extends HttpError {
  final val errorCode: Int = 404 // scalafix:ok DisableSyntax.noFinalVal
}

trait HttpInternalServerError extends HttpError {
  final val errorCode: Int = 500 // scalafix:ok DisableSyntax.noFinalVal
}

trait HttpServiceUnavailable extends HttpError {
  final val errorCode: Int = 503 // scalafix:ok DisableSyntax.noFinalVal
}
