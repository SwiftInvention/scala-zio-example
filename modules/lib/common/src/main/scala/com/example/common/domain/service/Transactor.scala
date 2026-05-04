package com.example.common.domain.service

import com.example.common.domain.model.Types.AppIO

/** Transaction boundary for repo operations.
  *
  * Default policy (see `tx-default` principle): every repo method opens a
  * transaction. App-service methods that orchestrate multiple repo calls may
  * wrap them in `withTransaction` — Quill's `transaction` is reentrant on a
  * fiber-local connection, so nesting reuses the outer scope.
  *
  * Errors: SQL exceptions are translated to `DbError` (an `AppFailure`). Repo
  * impls may `catchSome` for domain-meaningful constraints (e.g. unique
  * violations → `AlreadyExistsError`) before the transactor sees them.
  */
trait Transactor {
  def withTransaction[A](io: AppIO[A]): AppIO[A]
}
