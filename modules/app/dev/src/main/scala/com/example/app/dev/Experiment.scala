package com.example.app.dev

import zio._

/** A scratchpad for development-time experiments. Edit `experiment` to do the thing you want to try once.
  *
  * To run: `just experiment`.
  *
  * Layers: by default, no layers are provided beyond ZIO's built-ins (`Clock`, `Console`, `Random`, etc.). The
  * experiment doesn't connect to MySQL or load config until you ask it to — `just experiment` boots fast and works
  * without docker running. When you need infra, add the relevant `provide(...)` arguments below.
  *
  * For repeatable jobs that you might want to re-run, lift them out of here into `actions/` and add a dedicated `just`
  * recipe.
  */
object Experiment extends ZIOAppDefault {

  /** Edit this. */
  private val experiment: ZIO[Any, Throwable, Unit] =
    for {
      _ <- ZIO.unit
      // Add experiment effects here. Some examples:
      //
      //   _ <- ZIO.logInfo("hello from the scratchpad")
      //
      //   _ <- ZIO.sleep(200.millis)  // timing log will report ~200ms
      //
      // To exercise infra, add layers in `run`'s `provide(...)` and require them here:
      //   ctx <- ZIO.service[PgContext]
      //   _   <- ZIO.logInfo(s"datasource: ${ctx.ds}")
    } yield ()

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    (ZIO.logInfo("=== experiment start ===") *>
      timed(label = "experiment")(experiment))
      .ensuring(ZIO.logInfo("=== experiment end ==="))

  /** Wraps an effect with start/end timestamps and logs the elapsed time. Logs on success AND failure: `.exit` reifies
    * the outcome so timing is always observed, then we re-raise via `ZIO.refailCause` (preserving the original Cause
    * structure — defects, interrupts, and typed failures all flow through unchanged).
    */
  private def timed[R, E, A](label: String)(eff: ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      startedAt <- Clock.instant
      exit      <- eff.exit
      endedAt   <- Clock.instant
      ms     = java.time.Duration.between(startedAt, endedAt).toMillis
      status = if (exit.isSuccess) "ok" else "failed"
      _ <- ZIO.logInfo(s"$label took ${ms}ms ($status)")
      a <- exit match {
        case Exit.Success(value) => ZIO.succeed(value)
        case Exit.Failure(cause) => ZIO.refailCause(cause)
      }
    } yield a
}
