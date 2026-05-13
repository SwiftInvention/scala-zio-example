package com.example.ctx.customer.api

import com.example.ctx.customer.api.to.CustomerTO
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO

/** Cross-context contract for the customer ctx. TO-typed surface that other contexts call into. See
  * `patterns/cross-context-call.md`.
  */
trait CustomerApi {

  /** Fetch one customer. Fails with `CustomerNotFoundError` if missing. */
  def get(id: CustomerId): AppIO[CustomerTO]

  /** Batch lookup. The returned map's key set is a subset of `ids` — ids that don't resolve are absent. */
  def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, CustomerTO]]
}
