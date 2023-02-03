package org.organization

import java.io.IOException

import org.organization.AppEnv.AppEnv
import org.organization.http.AppServer
import org.organization.utils.db.Migration
import zio.{ZIOAppDefault, _}

object Application extends ZIOAppDefault {
  private val main: ZIO[AppEnv, Throwable, Nothing] = (
    ZIO.logInfo("Initializing application")
      *> Migration.migrate
      *> AppServer.serve
  )

  def run: IO[IOException, ExitCode] = for {
    executionResult <- main.provideLayer(AppEnv.buildLiveEnv).exit
    exitCode <- {
      executionResult match {
        case Exit.Success(_)     => ZIO.succeed(ExitCode.success)
        case Exit.Failure(error) => ZIO.logError(error.toString()) *> ZIO.succeed(ExitCode.failure)
      }
    }
  } yield exitCode
}
