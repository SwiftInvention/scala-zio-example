package org.organization

import org.organization.AppEnv.AppEnv
import org.organization.http.AppServer
import org.organization.utils.db.Migration
import zio.{ZIOAppDefault, _}

object Application extends ZIOAppDefault {
  private val main: ZIO[AppEnv, Throwable, Nothing] = (
    Console.printLine("Initializing application")
      *> Migration.migrate
      *> AppServer.serve
  )
  def run: UIO[ExitCode] =
    main.provideLayer(AppEnv.buildLiveEnv).exitCode
}
