package org.organization.http.check

import org.organization.db.repository.HealthCheckHelper
import org.organization.AppEnv.AppEnv
import sttp.tapir._
import zio._
import sttp.tapir.ztapir.ZTapir

object HealthCheckEndpoint extends HealthCheckHelper with ZTapir {

  val healthChecking: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint
    .name("Healthcheck-endpoint")
    .description("returns 200 if the database is available at the time the request is received")
    .get
    .in("check" / "health")
    .out(emptyOutput)

  val healthCheckingServerEndpoint: ZServerEndpoint[AppEnv, Any] =
    healthChecking.zServerLogic(_ => healthCheck.mapError(_ => ()))

  val readinessChecking: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint
    .name("Readiness-endpoint")
    .description("returns 200 if the server is available")
    .get
    .in("check" / "readiness")
    .out(emptyOutput)

  val readinessCheckingServerEndpoint: ZServerEndpoint[AppEnv, Any] =
    readinessChecking.zServerLogic(_ => ZIO.unit)

}
