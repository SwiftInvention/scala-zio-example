package com.example.customer.domain.service.repo

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Customer

trait CustomerRepo {
  def find(id: CustomerId): AppIO[Option[Customer]]
  def list: AppIO[List[Customer]]
}
