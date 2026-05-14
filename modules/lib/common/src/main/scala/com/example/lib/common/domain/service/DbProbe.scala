package com.example.lib.common.domain.service

import com.example.lib.common.domain.model.Types.AppIO

/** Reachability check for the readiness probe. The HTTP-server layer composes this with a timeout; the DB-backed impl
  * lives in `lib/db`.
  */
trait DbProbe {
  def ping: AppIO[Unit]
}
