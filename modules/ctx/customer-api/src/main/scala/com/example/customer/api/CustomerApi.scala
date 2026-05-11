package com.example.customer.api

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.to.CustomerTO

/** Cross-context contract for the customer ctx. TO-typed surface that other contexts call into.
  *
  * Holds only the operations real callers need. Customer's own HTTP routes go through `CustomerAppService` directly and
  * are not represented here. See `patterns/ctx-api.md` and `patterns/cross-context-call.md`.
  */
trait CustomerApi {

  /** Fetch one customer. Fails with `CustomerNotFoundError` if missing. Used for existence checks on the create paths
    * of other contexts.
    */
  def get(id: CustomerId): AppIO[CustomerTO]

  /** Batch lookup. The returned map's key set is a subset of `ids` — ids that don't resolve are absent. Callers decide
    * whether absence is a normal case or an error. Exists to keep N+1 fan-out out of cross-context list views.
    */
  def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, CustomerTO]]
}
