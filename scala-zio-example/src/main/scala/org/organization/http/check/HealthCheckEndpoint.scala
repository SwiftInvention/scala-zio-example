package org.organization.http.check

import org.organization.AppEnv.AppEnv
import org.organization.db.repository.HealthCheckHelper
import org.organization.http.BaseEndpoint.{makeEndpoint, makeEndpointHandler}
import org.organization.http.InternalServerError
import sttp.tapir._
import sttp.tapir.ztapir.ZTapir

object HealthCheckEndpoint extends HealthCheckHelper with ZTapir {

  val databaseCheck: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "database-health-check",
        "returns 200 if the database is available at the time the request is received"
      ).get
        .in("check" / "database")
        .out(emptyOutput)
    )(_ => databaseHealthCheck.mapError(_ => InternalServerError))

  val serverCheck: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "server-health-check",
        "returns 200 if the server is available"
      ).get
        .in("check" / "server")
        .out(emptyOutput)
    )(_ => databaseHealthCheck.mapError(_ => InternalServerError))

}
