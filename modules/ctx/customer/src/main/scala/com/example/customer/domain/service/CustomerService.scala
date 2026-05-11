package com.example.customer.domain.service

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Customer

trait CustomerService {
  def find(id: CustomerId): AppIO[Option[Customer]]
  def get(id: CustomerId): AppIO[Customer]
  def list: AppIO[List[Customer]]

  /** Batch lookup. Returned map omits any id that doesn't resolve. */
  def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]]
}
