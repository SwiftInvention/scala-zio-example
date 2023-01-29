package org.organization.http.swagger

import org.organization.AppEnv.AppEnv
import org.organization.BuildInfo
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
    PersonEndpoint.personList,
    PersonEndpoint.allPersonList,
    PersonEndpoint.personByIdentifier,
    PersonEndpoint.oldestPerson,
    PersonEndpoint.createPerson
  )

  private val apiEndpoints = healthCheckEndpoints ++ personEndpoints

  private val docEndpoints: List[ZServerEndpoint[AppEnv, Any]] = SwaggerInterpreter()
    .fromServerEndpoints(apiEndpoints, BuildInfo.name, BuildInfo.version)

  val common: List[SwaggerApiEndpoint.ZServerEndpoint[AppEnv, Any]] = apiEndpoints ++ docEndpoints
}
