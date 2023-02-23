package org.organization.integration_tests.util

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import io.circe.Encoder
import io.circe.syntax.EncoderOps
import zio.stream.{ZPipeline, ZSink, ZStream}
import zio.test._
import zio.{UIO, ZIO}

trait SnapshotSpec {
  private val snapshotDirectory     = "../snapshots/"
  private val snapshotDirectoryPath = Paths.get(snapshotDirectory)

  private def createSnapshotDirectoryIfNotExist = ZIO
    .attempt(Files.createDirectory(snapshotDirectoryPath))
    .when(!Files.exists(snapshotDirectoryPath))
    .orDie

  private def readFromFile(path: Path) =
    ZStream
      .fromPath(path)
      .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
      .run(ZSink.collectAll[String].map(_.mkString("\n")))

  private def writeToFile(path: Path, content: String) =
    ZStream
      .fromIterable(content.getBytes(StandardCharsets.UTF_8))
      .run(ZSink.fromPath(path))

  private def readExistingOrCreate(fileName: String, actualContent: String): UIO[String] = {
    val snapshotPath = snapshotDirectory ++ fileName
    val path         = Paths.get(snapshotPath)

    if (Files.exists(path)) {
      readFromFile(path).logError("Reading file error").orDie
    } else {
      println(s"Snapshot '$fileName' doesn't exist and will be created")
      for {
        _ <- ZIO.attempt(Files.createDirectories(path.getParent)).orDie
        _ <- ZIO.attempt(Files.createFile(path)).orDie
        _ <- writeToFile(path, actualContent).logError("Writing file error").orDie
      } yield actualContent
    }
  }

  /** Encodes arbitrary data using Circe
    *
    * Note: Sorting keys makes snapshots reproducible
    */
  def matchesJsonSnapshot[A](fileName: String, result: A)(implicit encoder: Encoder[A]): UIO[TestResult] =
    assertMatchesSnapshot(fileName, result.asJson.spaces2SortKeys)

  /** Takes string as snapshot body */
  private def assertMatchesSnapshot(fileName: String, actualContent: String): UIO[TestResult] = for {
    _               <- createSnapshotDirectoryIfNotExist
    expectedContent <- readExistingOrCreate(fileName, actualContent)
  } yield assertTrue(actualContent equals expectedContent)

}
