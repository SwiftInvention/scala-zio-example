package org.organization.http.swagger

import org.organization.AppEnv.AppEnv
import org.organization.http.PersonEndpoint
import org.organization.http.check.HealthCheckEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.ZTapir

object SwaggerApiEndpoint extends ZTapir {
  private val healthCheckEndpoints = List(
    HealthCheckEndpoint.serverCheck,
    HealthCheckEndpoint.databaseCheck
  )

  private val personEndpoints = List(
    PersonEndpoint.personListing,
    PersonEndpoint.personByIdentifier,
    PersonEndpoint.createPerson
  )

  private val apiEndpoints = healthCheckEndpoints ++ personEndpoints

  private val docEndpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerInterpreter()
    .fromServerEndpoints(apiEndpoints, "project", "0.1.0")

  val common: List[SwaggerApiEndpoint.ZServerEndpoint[AppEnv, Any]] = apiEndpoints ++ docEndpoints
}
