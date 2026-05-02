package com.example.common.domain.error.api

/** Self-typed onto `AppFailure` to carry an HTTP status code with a typed error.
  *
  * Each concrete error mixes in one of the status traits below. The route boundary
  * reads `errorCode` to set the response status.
  */
sealed trait HttpError {
  val errorCode: Int
}

trait HttpBadRequest extends HttpError {
  val errorCode = 400
}

trait HttpUnauthorized extends HttpError {
  val errorCode = 401
}

trait HttpForbidden extends HttpError {
  val errorCode = 403
}

trait HttpNotFound extends HttpError {
  val errorCode = 404
}

trait HttpInternalServerError extends HttpError {
  val errorCode = 500
}

trait HttpServiceUnavailable extends HttpError {
  val errorCode = 503
}
