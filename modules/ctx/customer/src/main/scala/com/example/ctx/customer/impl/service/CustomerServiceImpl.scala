package com.example.ctx.customer.impl.service

import com.example.ctx.customer.domain.error.CustomerNotFoundError
import com.example.ctx.customer.domain.model.Customer
import com.example.ctx.customer.domain.service.CustomerService
import com.example.ctx.customer.domain.service.repo.CustomerRepo
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Pass-through to `CustomerRepo`. Domain-level logic (validation, business rules, cross-aggregate orchestration) lands
  * here as it accrues.
  */
final class CustomerServiceImpl(repo: CustomerRepo) extends CustomerService {
  override def find(id: CustomerId): AppIO[Option[Customer]] = repo.find(id)

  override def get(id: CustomerId): AppIO[Customer] =
    repo.find(id).someOrFail(CustomerNotFoundError.withId(id))

  override def list: AppIO[List[Customer]] = repo.list

  override def getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, Customer]] =
    if (ids.isEmpty) ZIO.logDebug("empty id set, returning empty map").as(Map.empty)
    else repo.findMany(ids).map(_.iterator.map(c => c.id -> c).toMap)
}

object CustomerServiceImpl {
  val layer: URLayer[CustomerRepo, CustomerService] =
    ZLayer.fromFunction(new CustomerServiceImpl(_))
}
