package com.example.customer.impl.service.repo

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.customer.domain.model.Customer
import com.example.customer.domain.service.repo.CustomerRepo
import zio._

/** Stub repo with hardcoded data. Replaced by a DB-backed impl in a later step. */
final class CustomerRepoImpl extends CustomerRepo {
  private val stubs: List[Customer] = List(
    Customer(CustomerId("c-001"), "ada@example.com",   "Ada Lovelace"),
    Customer(CustomerId("c-002"), "alan@example.com",  "Alan Turing"),
    Customer(CustomerId("c-003"), "grace@example.com", "Grace Hopper")
  )

  override def find(id: CustomerId): AppIO[Option[Customer]] =
    ZIO.succeed(stubs.find(_.id == id))

  override def list: AppIO[List[Customer]] =
    ZIO.succeed(stubs)
}

object CustomerRepoImpl {
  val layer: ULayer[CustomerRepo] =
    ZLayer.succeed(new CustomerRepoImpl)
}
