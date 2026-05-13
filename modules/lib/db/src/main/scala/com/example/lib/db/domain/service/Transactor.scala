package com.example.lib.db.domain.service

import com.example.lib.common.domain.model.Types.AppIO

/** Transaction boundary for repo operations.
  *
  * Every repo method opens a transaction. Service methods that orchestrate multiple repo calls may wrap them in
  * `withTransaction` — nested calls reuse the outer connection (Quill's `transaction` is reentrant on a fiber-local
  * connection), so nesting doesn't open a second SQL transaction.
  *
  * Errors: SQL exceptions are translated to `DbError` (an `AppFailure`). Repo impls may `catchSome` for
  * domain-meaningful constraints (e.g. unique violations → `AlreadyExistsError`) before the transactor sees them.
  */
trait Transactor {
  def withTransaction[A](io: AppIO[A]): AppIO[A]
}
