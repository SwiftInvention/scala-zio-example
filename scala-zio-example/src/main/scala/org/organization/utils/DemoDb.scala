package org.organization.utils

import org.organization.AppEnv
import org.organization.utils.db.Migration
import zio._

// Executable feature to fill db by demo data
object DemoDb extends App {
  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    (Migration.migrate *> DemoData.fillDb).provideLayer(AppEnv.buildLiveEnv).exitCode
  }
}