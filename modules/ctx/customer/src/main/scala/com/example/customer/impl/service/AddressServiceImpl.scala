package com.example.customer.impl.service

import com.example.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.error.AddressNotFoundError
import com.example.customer.domain.model.Address
import com.example.customer.domain.service.AddressService
import com.example.customer.domain.service.repo.AddressRepo
import zio._

/** Pass-through to `AddressRepo` plus `get` (fail-on-missing). Domain-level logic accrues here. */
final class AddressServiceImpl(repo: AddressRepo) extends AddressService {
  override def find(id: AddressId): AppIO[Option[Address]] = repo.find(id)

  override def get(id: AddressId): AppIO[Address] =
    repo.find(id).someOrFail(AddressNotFoundError.withId(id))

  override def listForCustomer(customerId: CustomerId): AppIO[List[Address]] =
    repo.listForCustomer(customerId)
}

object AddressServiceImpl {
  val layer: URLayer[AddressRepo, AddressService] =
    ZLayer.fromFunction(new AddressServiceImpl(_))
}
