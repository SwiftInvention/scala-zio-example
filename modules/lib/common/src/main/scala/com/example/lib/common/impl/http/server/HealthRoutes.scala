package com.example.lib.common.impl.http.server

import com.example.lib.common.domain.error.AppFailure
import com.example.lib.common.domain.error.backend.ProbeTimeoutError
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.common.domain.service.DbProbe
import com.example.lib.common.impl.http.{ApiFailure, ErrorTO}
import com.example.lib.common.impl.logging.LogError
import zio._
import zio.http._

/** Operational health probes shared across server deployments. Two endpoints with different semantics:
  *
  *   - `GET /health` — liveness: the process is up and the HTTP layer responds. Always 200. Wire as a k8s
  *     `livenessProbe` — failing it should restart the process.
  *   - `GET /ready` — readiness: required dependencies (DB) are reachable. 200 on success, 503 (with `ErrorTO` body)
  *     otherwise. Wire as a k8s `readinessProbe` — failing it should remove the pod from load-balancing without
  *     restarting.
  *
  * Reachability is delegated to `DbProbe`; the route owns the timeout bound and the response shape.
  */
final class HealthRoutes(dbProbe: DbProbe) {

  // architecture: config-shape FIXME — should come from a HealthConfig rather than a literal in code.
  private val ProbeTimeout: Duration = 2.seconds

  // Any pingDb failure means "not ready" — wrap as `ServiceUnavailableResponse` regardless of the underlying
  // AppFailure's HTTP status. The readiness probe's contract is binary: ready or not ready.
  private def asNotReady(f: AppFailure): ApiFailure.ServiceUnavailableResponse =
    ApiFailure.ServiceUnavailableResponse(ErrorTO.from(f))

  private val timedOutFailure: ApiFailure.ServiceUnavailableResponse =
    asNotReady(ProbeTimeoutError("Readiness probe timed out", None))

  private val pingDb: AppIO[Unit] = dbProbe.ping

  private val health =
    HealthEndpoints.health.implement { (_: Unit) => ZIO.unit }

  private val ready =
    HealthEndpoints.ready.implement { (_: Unit) =>
      pingDb
        .timeout(ProbeTimeout)
        .tapError(LogError.tagged("HealthRoutes.ready"))
        .foldZIO(
          failure = f => ZIO.fail(asNotReady(f)),
          success = {
            case Some(_) => ZIO.unit
            case None    => ZIO.fail(timedOutFailure)
          }
        )
    }

  val routes: Routes[Any, Response] =
    health.toRoutes ++ ready.toRoutes
}

object HealthRoutes {
  val layer: URLayer[DbProbe, HealthRoutes] =
    ZLayer.fromFunction(new HealthRoutes(_))
}
