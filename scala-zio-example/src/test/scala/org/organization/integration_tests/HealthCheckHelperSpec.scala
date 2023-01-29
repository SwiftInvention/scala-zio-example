package org.organization.integration_tests

import javax.sql.DataSource

import org.organization.db.repository.HealthCheckHelper
import org.organization.integration_tests.util.DatabaseIntegrationSpec
import zio.test._

object HealthCheckHelperSpec extends DatabaseIntegrationSpec with HealthCheckHelper {

  def integrationSpec: Spec[DataSource, Throwable] =
    suite("HealthCheckHelper")(
      test("health check succeeds") {
        for {
          healthCheckResult <- databaseHealthCheck
        } yield assert(healthCheckResult)(Assertion.equalTo(()))
      }
    )
}
