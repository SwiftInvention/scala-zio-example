package com.example.common.domain.model

import com.example.common.domain.error.AppFailure
import zio._

/** Effect-level type aliases used across the codebase.
  *
  * The error channel is `AppFailure` — every failure flowing through `AppIO` is one of our structured errors. Raw
  * `Throwable`s from JVM/JDBC/library boundaries do not enter the channel implicitly: they are wrapped at the boundary
  * via explicit `mapError` (see `Transactor`, `PgContext`, `DataSourceLayer`). This keeps the invariant "every error in
  * the app is one we defined" compiler-checked.
  */
object Types {
  type AppIO[+A]      = IO[AppFailure, A]
  type AppRIO[-R, +A] = ZIO[R, AppFailure, A]
}
