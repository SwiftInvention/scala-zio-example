package org.organization.http.swagger

import org.organization.AppEnv.AppEnv
import org.organization.http.PersonEndpoint.{personByIdentifierServerLogic, personListingServerLogic}
import org.organization.http.check.HealthCheckEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZTapir

object SwaggerApiEndpoint extends ZTapir {
  private val apiEndpoints: List[ZServerEndpoint[AppEnv, Any]] =
    List(
      HealthCheckEndpoint.healthCheckingServerEndpoint,
      HealthCheckEndpoint.readinessCheckingServerEndpoint,
      personListingServerLogic,
      personByIdentifierServerLogic
    )

  private val docEndpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerInterpreter()
    .fromServerEndpoints(apiEndpoints, "project", "0.1.0")

  val common: List[SwaggerApiEndpoint.ZServerEndpoint[AppEnv, Any]] = apiEndpoints ++ docEndpoints
}
