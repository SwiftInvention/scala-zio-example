package com.example.common.impl.config

import com.example.common.domain.model.EnvLabel
import com.typesafe.config.ConfigFactory
import pureconfig.{ConfigReader, ConfigSource}
import zio._

/** Reads `APP_ENV` and parses to `EnvLabel`. The label is the only thing that propagates downstream — typesafe-config
  * and pureconfig stay inside this file and the per-config `XConfig.load` calls.
  *
  * Fail-fast at boot: unset/invalid `APP_ENV`, missing/empty conf resource, and pureconfig parse failures all surface
  * as layer-construction errors and the app refuses to start.
  *
  * Per the `config-shape` principle, conf files are self-contained per (app, env).
  */
object ConfigBootstrap {

  /** Bootstrap layer — produces just the parsed `EnvLabel`. Downstream `XConfig.layer`s depend on `EnvLabel` and call
    * `ConfigBootstrap.load` to parse their own slice.
    */
  val layer: TaskLayer[EnvLabel] = ZLayer.fromZIO {
    for {
      raw <- ZIO
        .fromOption(sys.env.get("APP_ENV"))
        .orElseFail(new RuntimeException(s"APP_ENV is not set; expected one of $validNames"))
      label <- ZIO
        .fromOption(EnvLabel.withNameInsensitiveOption(raw))
        .orElseFail(new RuntimeException(s"APP_ENV='$raw' is not a valid EnvLabel: $validNames"))
      _ <- ZIO.logInfo(s"APP_ENV resolved: ${label.entryName}")
    } yield label
  }

  /** Load and parse a typed config slice from `application-<label>.conf`.
    *
    * Used in each `XConfig.scala` — keeps the typesafe-config + pureconfig call contained to the parsing tier.
    * Downstream service layers consume the returned typed value and never see `Config`.
    */
  def load[T: ConfigReader](path: String): ZIO[EnvLabel, Throwable, T] =
    ZIO.serviceWithZIO[EnvLabel] { label =>
      val resource = s"application-${label.entryName}.conf"
      for {
        cfg <- ZIO
          .attempt(ConfigFactory.parseResources(resource).resolve())
          .filterOrFail(_.entrySet().size() > 0)(
            new RuntimeException(s"Config resource '$resource' is empty or missing on classpath")
          )
        result <- ZIO
          .fromEither(ConfigSource.fromConfig(cfg).at(path).load[T])
          .mapError(fs => new RuntimeException(s"Config '$path' load failed: ${fs.toList.mkString("; ")}"))
      } yield result
    }

  private val validNames: String = EnvLabel.values.map(_.entryName).mkString("|")
}
