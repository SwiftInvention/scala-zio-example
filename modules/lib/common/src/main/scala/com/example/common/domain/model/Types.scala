package com.example.common.domain.model

import zio._

/** Effect-level type aliases used across the codebase. */
object Types {
  type AppIO[+A]      = IO[Throwable, A]
  type AppRIO[-R, +A] = ZIO[R, Throwable, A]
}
