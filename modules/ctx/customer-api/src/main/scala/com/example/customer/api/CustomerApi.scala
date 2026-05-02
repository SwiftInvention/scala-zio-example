package com.example.customer.api

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.api.to.CustomerTO

trait CustomerApi {
  def find(id: CustomerId): AppIO[Option[CustomerTO]]
  def get(id: CustomerId): AppIO[CustomerTO]
  def list: AppIO[List[CustomerTO]]
}
