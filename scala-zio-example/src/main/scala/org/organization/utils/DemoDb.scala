package org.organization.utils

import java.io.IOException

import org.organization.AppEnv
import org.organization.utils.db.Migration
import zio.{ZIOAppDefault, _}

// Executable feature to fill db by demo data
object DemoDb extends ZIOAppDefault {
  def run: IO[IOException, ExitCode] = {
    (Migration.migrate *> DemoData.fillDb).provideLayer(AppEnv.buildLiveEnv).exitCode
  }
}
