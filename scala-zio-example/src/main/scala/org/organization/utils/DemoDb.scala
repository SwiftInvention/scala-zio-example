package org.organization.utils

import org.organization.AppEnv
import org.organization.utils.db.Migration
import zio.ZIOAppDefault

// Executable feature to fill db by demo data
object DemoDb extends ZIOAppDefault {
  def run =
    (Migration.migrate *> DemoData.fillDb).provideLayer(AppEnv.buildLiveEnv)
}
