package org.organization.integration_tests

import org.organization.db.repository.HealthCheckHelper
import org.organization.integration_tests.util.DatabaseIntegrationSpec
import zio.test._

import javax.sql.DataSource

object HealthCheckHelperSpec extends DatabaseIntegrationSpec with HealthCheckHelper {

  def integrationSpec: ZSpec[DataSource, Throwable] =
    suite("HealthCheckHelper")(
      test("health check succeeds") {
        for {
          healthCheckResult <- databaseHealthCheck
        } yield assert(healthCheckResult)(Assertion.equalTo(()))
      }
    )
}
