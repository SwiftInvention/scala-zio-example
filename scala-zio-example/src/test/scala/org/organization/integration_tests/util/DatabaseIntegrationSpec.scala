package org.organization.integration_tests.util

import com.dimafeng.testcontainers.MySQLContainer
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import org.organization.integration_tests.util.CustomMySQLContainer.Settings
import zio._
import zio.test._
import zio.test.environment.TestEnvironment

import javax.sql.DataSource

/** Spins up a separate postgres container for each test. Limits the amount of tests running in
  * parallel to put an upper bound on resource usage
  */
abstract class DatabaseIntegrationSpec extends DefaultRunnableSpec {

  type IntegrationTestEnv = Has[DataSource] with TestEnvironment

  val mysqlVersion     = "8.0"
  val parallelismLimit = 4

  private val containerLayer = ZLayer.succeed(
    Settings(
      mysqlVersion,
      MySQLContainer.defaultDatabaseName,
      MySQLContainer.defaultUsername,
      MySQLContainer.defaultPassword
    )
  ) >+> CustomMySQLContainer.live

  final def spec: ZSpec[TestEnvironment, Any] =
    (integrationSpec
      @@ TestAspect.parallelN(parallelismLimit)
      @@ DbMigrationAspect.migrate()()).provideSomeLayer[TestEnvironment](containerLayer.fresh)
  def integrationSpec: ZSpec[IntegrationTestEnv, Any]

}
