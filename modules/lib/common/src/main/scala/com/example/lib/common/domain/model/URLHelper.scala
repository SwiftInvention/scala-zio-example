package com.example.lib.common.domain.model

import com.example.lib.common.domain.error.domain.InvalidURLError
import com.example.lib.common.domain.model.Types.AppIO
import zio._
import zio.http.URL

/** Smart-constructor-style helper for `zio.http.URL`. Lifts `URL.decode`'s native `Either[Exception, URL]` into the
  * codebase's typed channels — `AppIO[URL]` at runtime call sites, `Either[InvalidURLError, URL]` at PureConfig
  * `ConfigReader` boundaries.
  *
  * Use anywhere an HTTP/HTTPS URL needs to be parsed from a String — config readers, request-body decoders, untyped
  * inbound boundaries. Downstream code holds `URL` and trusts the type; no repeated `URL.decode` at use sites.
  */
object URLHelper {

  def parse(s: String): AppIO[URL] =
    ZIO.fromEither(parseEither(s))

  def parseEither(s: String): Either[InvalidURLError, URL] =
    URL.decode(s).left.map(t => InvalidURLError(message = s"Invalid URL '$s': ${t.getMessage}"))
}
