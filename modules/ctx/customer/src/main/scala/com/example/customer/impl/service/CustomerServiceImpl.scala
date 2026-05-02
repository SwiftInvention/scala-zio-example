package com.example.customer.impl.service

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.error.CustomerNotFoundError
import com.example.customer.domain.model.Customer
import com.example.customer.domain.service.CustomerService
import com.example.customer.domain.service.repo.CustomerRepo
import zio._

/** Pass-through to `CustomerRepo`. Domain-level logic (validation, business rules,
  * cross-aggregate orchestration) lands here as it accrues.
  */
final class CustomerServiceImpl(repo: CustomerRepo) extends CustomerService {
  override def find(id: CustomerId): AppIO[Option[Customer]] = repo.find(id)

  override def get(id: CustomerId): AppIO[Customer] =
    repo.find(id).someOrFail(CustomerNotFoundError.withId(id))

  override def list: AppIO[List[Customer]] = repo.list
}

object CustomerServiceImpl {
  val layer: URLayer[CustomerRepo, CustomerService] =
    ZLayer.fromFunction(new CustomerServiceImpl(_))
}
