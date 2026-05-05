package com.example.common.test

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import zio._
import zio.json._
import zio.test._

/** JSON snapshot ("golden") testing helpers, mixed into a `ZIOSpecDefault`.
  *
  * Behavior:
  *   - On first run for a given `name`, no snapshot exists — write the current actual content to disk and pass the
  *     assertion. The new file is meant to be reviewed and committed.
  *   - On subsequent runs, read the snapshot from disk and compare to actual. Differences fail the assertion.
  *   - With `SNAPSHOT_UPDATE=true` (env var) or `-Dsnapshot.update=true` (sys prop), always overwrite. Use this
  *     intentionally when the wire shape has changed and the change has been reviewed.
  *
  * Snapshots live at `<module>/src/test/resources/snapshots/<name>` — resolved relative to the test process's working
  * directory, which is the module's `baseDirectory` when `Test / fork := true` (set in `commonSettings`).
  *
  * `name` is author-supplied (e.g. `"CustomerTO/single.json"`). Subdirectories are created on demand.
  */
trait SnapshotSpec {

  private val snapshotRoot = Paths.get("src/test/resources/snapshots")

  private val updateMode: Boolean =
    sys.env.get("SNAPSHOT_UPDATE").contains("true") ||
      sys.props.get("snapshot.update").contains("true")

  /** Snapshot a value as pretty-printed JSON (2-space indent), via zio-json. */
  def matchesJsonSnapshot[A: JsonEncoder](name: String, value: A): UIO[TestResult] =
    matchesStringSnapshot(name = name, actual = value.toJsonPretty)

  private def matchesStringSnapshot(name: String, actual: String): UIO[TestResult] = {
    val path = snapshotRoot.resolve(name)
    val ensureParent =
      ZIO.attemptBlocking(Files.createDirectories(path.getParent)).orDie
    val write =
      ZIO.attemptBlocking(Files.writeString(path, actual, StandardCharsets.UTF_8)).orDie
    val read =
      ZIO.attemptBlocking(Files.readString(path, StandardCharsets.UTF_8)).orDie
    val exists = ZIO.attemptBlocking(Files.exists(path)).orDie

    for {
      _       <- ensureParent
      isThere <- exists
      result <-
        if (updateMode) {
          write.as(assertCompletes)
        } else if (!isThere) {
          ZIO.logInfo(s"Snapshot '$name' missing — recording actual output for review") *>
            write.as(assertCompletes)
        } else {
          read.map(expected => assertTrue(actual == expected)) // scalafix:ok DisableSyntax.==
        }
    } yield result
  }
}
