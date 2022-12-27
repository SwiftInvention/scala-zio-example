package org.organization.http.check

import org.organization.AppEnv.AppEnv
import org.organization.db.repository.HealthCheckHelper
import org.organization.http.BaseEndpoint.{makeEndpoint, makeEndpointHandler}
import org.organization.http.InternalServerError
import sttp.tapir._
import sttp.tapir.ztapir.ZTapir
import zio.ZIO

object HealthCheckEndpoint extends HealthCheckHelper with ZTapir {

  val databaseCheck: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "Healthcheck-endpoint",
        "returns 200 if the database is available at the time the request is received"
      ).get
        .in("check" / "health")
        .out(emptyOutput)
    )(_ => databaseHealthCheck.mapError(_ => InternalServerError))

  val serverCheck: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "Readiness-endpoint",
        "returns 200 if the server is available"
      ).get
        .in("check" / "readiness")
        .out(emptyOutput)
    )(_ => ZIO.unit)

}
