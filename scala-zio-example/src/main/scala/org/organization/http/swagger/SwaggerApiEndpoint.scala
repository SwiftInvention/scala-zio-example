package org.organization.http.swagger

import org.organization.http.PersonEndpoint.personListingServerLogic
import org.organization.http.check.HealthCheckEndpoint
import org.organization.AppEnv.AppEnv
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZTapir

object SwaggerApiEndpoint extends ZTapir {
  val apiEndpoints: List[ZServerEndpoint[AppEnv, Any]] =
    List(
      HealthCheckEndpoint.healthCheckingServerEndpoint,
      HealthCheckEndpoint.readinessCheckingServerEndpoint,
      personListingServerLogic
    )

  val docEndpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerInterpreter()
    .fromServerEndpoints(apiEndpoints, "project", "0.1.0")

  val common = apiEndpoints ++ docEndpoints
}
