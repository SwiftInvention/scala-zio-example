package org.organization

import org.organization.AppEnv.AppEnv
import org.organization.http.AppServer
import org.organization.utils.db.Migration
import zio.{ZIOAppDefault, _}

import java.io.IOException

object Application extends ZIOAppDefault {
  private val main: ZIO[AppEnv, Throwable, Nothing] = (
    Console.printLine("Initializing application")
      *> Migration.migrate
      *> AppServer.serve
  )

  def run: IO[IOException, ExitCode] = for {
    executionResult <- main.provideLayer(AppEnv.buildLiveEnv).exit
    exitCode <- {
      executionResult match {
        case Exit.Success(_)     => ZIO.succeed(ExitCode.success)
        case Exit.Failure(error) => Console.printLine(error) *> ZIO.succeed(ExitCode.failure)
      }
    }
  } yield exitCode
}
