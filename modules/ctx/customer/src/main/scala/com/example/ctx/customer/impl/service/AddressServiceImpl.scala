package com.example.ctx.customer.impl.service

import com.example.ctx.customer.domain.error.AddressNotFoundError
import com.example.ctx.customer.domain.model.Address
import com.example.ctx.customer.domain.service.AddressService
import com.example.ctx.customer.domain.service.repo.AddressRepo
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO
import zio._

/** Pass-through to `AddressRepo` with `get` lifting absence to `AddressNotFoundError`. */
final class AddressServiceImpl(repo: AddressRepo) extends AddressService {
  override def get(id: AddressId): AppIO[Address] =
    repo.find(id).someOrFail(AddressNotFoundError.withId(id))

  override def listForCustomer(customerId: CustomerId): AppIO[List[Address]] =
    repo.listForCustomer(customerId)
}

object AddressServiceImpl {
  val layer: URLayer[AddressRepo, AddressService] =
    ZLayer.fromFunction(new AddressServiceImpl(_))
}
