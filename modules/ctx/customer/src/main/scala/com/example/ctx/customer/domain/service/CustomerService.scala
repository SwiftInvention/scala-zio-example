package com.example.ctx.customer.domain.service

import com.example.ctx.customer.domain.model.Customer
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO

trait CustomerService {
  def find(id: CustomerId): AppIO[Option[Customer]]
  def get(id: CustomerId): AppIO[Customer]
  def list: AppIO[List[Customer]]

  /** Batch lookup. Returned map omits any id that doesn't resolve. */
  def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]]
}
