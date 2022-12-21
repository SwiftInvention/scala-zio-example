package org.organization.http

sealed trait ErrorForHttpClient
case object NotFound                                extends ErrorForHttpClient
final case class BadRequest(messageForUser: String) extends ErrorForHttpClient
case object Unauthorized                            extends ErrorForHttpClient
case object InternalServerError                     extends ErrorForHttpClient
