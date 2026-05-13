package com.example.ctx.customer.domain.service.repo

import com.example.ctx.customer.domain.model.Customer
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO

trait CustomerRepo {
  def find(id: CustomerId): AppIO[Option[Customer]]
  def list: AppIO[List[Customer]]
  def findMany(ids: Set[CustomerId]): AppIO[List[Customer]]
}
