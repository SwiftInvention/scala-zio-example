package com.example.customer.impl.service.repo

import com.example.common.domain.model.NewTypes.CustomerId
import com.example.common.domain.model.Types.AppIO
import com.example.common.domain.service.Transactor
import com.example.common.impl.repo.pg.PgContext
import com.example.customer.domain.model.Customer
import com.example.customer.domain.service.repo.CustomerRepo
import com.example.customer.impl.service.repo.pg.CustomerDbSchema
import com.example.customer.impl.service.repo.pg.converter.CustomerPEConverter
import zio._

/** MySQL-backed `CustomerRepo`.
  *
  * Each method opens its own transaction via `Transactor.withTransaction`
  * (per the `tx-default` principle). App-service-level wrapping nests safely
  * — Quill's `transaction` is reentrant on a fiber-local connection.
  *
  * Schema is assumed to exist — migrations run out-of-process via `just db-migrate`.
  */
final class CustomerRepoMySQLImpl(val ctx: PgContext, transactor: Transactor)
    extends CustomerRepo
    with CustomerDbSchema {
  import ctx._

  override def find(id: CustomerId): AppIO[Option[Customer]] =
    transactor.withTransaction {
      val q = quote(customerTable.filter(_.id == lift(id)))
      ctx.runQuerySingleResult(run(q)).map(_.map(CustomerPEConverter.toCustomer))
    }

  override def list: AppIO[List[Customer]] =
    transactor.withTransaction {
      val q = quote(customerTable)
      ctx.runQuery(run(q)).map(_.map(CustomerPEConverter.toCustomer))
    }
}

object CustomerRepoMySQLImpl {
  val layer: URLayer[PgContext & Transactor, CustomerRepo] =
    ZLayer.fromFunction(new CustomerRepoMySQLImpl(_, _))
}
