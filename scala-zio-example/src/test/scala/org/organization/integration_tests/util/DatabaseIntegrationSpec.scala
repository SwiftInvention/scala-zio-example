package org.organization.integration_tests.util

import javax.sql.DataSource

import com.dimafeng.testcontainers.MySQLContainer
import io.github.scottweaver.zio.aspect.DbMigrationAspect
import io.github.scottweaver.zio.testcontainers.mysql.ZMySQLContainer
import io.github.scottweaver.zio.testcontainers.mysql.ZMySQLContainer.Settings
import zio._
import zio.test.{ZIOSpecDefault, _}

/** Spins up a separate MySQL container for each test. Limits the number of tests running in
  * parallel to put an upper bound on resource usage
  */
abstract class DatabaseIntegrationSpec extends ZIOSpecDefault {

  type IntegrationTestEnv = DataSource with TestEnvironment

  val mysqlVersion     = "8.0"
  val parallelismLimit = 4

  private val containerLayer = ZLayer.succeed(
    Settings(
      mysqlVersion,
      MySQLContainer.defaultDatabaseName,
      MySQLContainer.defaultUsername,
      MySQLContainer.defaultPassword
    )
  ) >+> ZMySQLContainer.live

  final def spec: Spec[TestEnvironment, Any] =
    (integrationSpec
      @@ TestAspect.parallelN(parallelismLimit)
      @@ DbMigrationAspect.migrate()())
      .provideSome[TestEnvironment](containerLayer)
  def integrationSpec: Spec[IntegrationTestEnv, Any]

}
