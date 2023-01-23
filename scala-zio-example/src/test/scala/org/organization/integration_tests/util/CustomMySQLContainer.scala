package org.organization.integration_tests.util

import com.dimafeng.testcontainers.MySQLContainer
import com.mysql.cj.jdbc.MysqlDataSource
import io.github.scottweaver.models.JdbcInfo
import org.testcontainers.utility.DockerImageName
import zio._

import java.sql.{Connection, DriverManager}
import javax.sql.DataSource
import zio.managed._

/** Copied from https://github.com/scottweaver/testcontainers-for-zio, made minor changes to fix a
  * type inference issue
  */
object CustomMySQLContainer {

  final case class Settings(
      imageVersion: String,
      databaseName: String,
      username: String,
      password: String
  )

  object Settings {
    val default: ULayer[Settings] = ZLayer.succeed(
      Settings(
        "latest",
        MySQLContainer.defaultDatabaseName,
        MySQLContainer.defaultUsername,
        MySQLContainer.defaultPassword
      )
    )
  }

  type Provides = JdbcInfo
    with Connection with AutoCloseable
    with DataSource
    with MySQLContainer

  val live: ZLayer[Settings, Nothing, Provides] = {

    def makeManagedConnection(
        container: MySQLContainer
    ): ZManaged[Any, Nothing, Connection with AutoCloseable] =
      ZManaged.acquireReleaseWith(
        ZIO.attempt {
          DriverManager.getConnection(
            container.jdbcUrl,
            container.username,
            container.password
          )
        }.orDie
      )(conn =>
        ZIO
          .attempt(conn.close())
          .tapError(err => ZIO.attempt(println(s"Error closing connection: $err")))
          .ignore
      )

    def makeManagedContainer(settings: Settings) =
      ZManaged.acquireReleaseWith(
        ZIO.attempt {
          val containerDef = MySQLContainer.Def(
            dockerImageName = DockerImageName.parse(s"mysql:${settings.imageVersion}"),
            databaseName = settings.databaseName,
            username = settings.username,
            password = settings.password
          )
          containerDef.start()
        }.orDie
      )(container =>
        ZIO
          .attempt(container.stop())
          .tapError(err => ZIO.attempt(println(s"Error stopping container: $err")))
          .ignore
      )

    ZLayer.fromManagedMany {
      for {
        settings  <- ZIO.service[Settings].toManaged
        container <- makeManagedContainer(settings)
        conn      <- makeManagedConnection(container)

      } yield {

        val jdbcInfo = JdbcInfo(
          driverClassName = container.driverClassName,
          jdbcUrl = container.jdbcUrl,
          username = container.username,
          password = container.password
        )
        val dataSource = new MysqlDataSource()
        dataSource.setUrl(container.jdbcUrl)
        dataSource.setUser(container.username)
        dataSource.setPassword(container.password)

        Has(jdbcInfo) ++ Has(conn) ++ Has[DataSource](dataSource) ++ Has(container)
      }
    }
  }
}
