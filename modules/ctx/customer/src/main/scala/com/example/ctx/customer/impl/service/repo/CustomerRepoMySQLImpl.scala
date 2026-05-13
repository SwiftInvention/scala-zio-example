package com.example.ctx.customer.impl.service.repo

import com.example.ctx.customer.domain.model.Customer
import com.example.ctx.customer.domain.service.repo.CustomerRepo
import com.example.ctx.customer.impl.service.repo.converter.CustomerPEConverter
import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.sql.SqlContext
import com.example.lib.db.impl.sql.schema.CustomerDbSchema
import zio._

/** MySQL-backed `CustomerRepo`.
  *
  * Each method opens its own transaction via `Transactor.withTransaction` (per the `tx-default` principle).
  * App-service-level wrapping nests safely — Quill's `transaction` is reentrant on a fiber-local connection.
  *
  * Schema is assumed to exist — migrations run out-of-process via `just db-migrate`.
  */
final class CustomerRepoMySQLImpl(val ctx: SqlContext, transactor: Transactor)
    extends CustomerRepo
    with CustomerDbSchema {
  import ctx._

  import ctx.extras._

  override def find(id: CustomerId): AppIO[Option[Customer]] =
    transactor.withTransaction {
      val q = quote(customerTable.filter(_.id === lift(id)))
      ctx.runQuerySingleResult(run(q)).flatMap {
        case Some(pe) => CustomerPEConverter.toCustomer(pe).map(Some(_))
        case None     => ZIO.none
      }
    }

  override def list: AppIO[List[Customer]] =
    transactor.withTransaction {
      val q = quote(customerTable)
      ctx.runQuery(run(q)).flatMap(ZIO.foreach(_)(CustomerPEConverter.toCustomer))
    }

  override def findMany(ids: Set[CustomerId]): AppIO[List[Customer]] =
    if (ids.isEmpty) ZIO.succeed(Nil)
    else
      transactor.withTransaction {
        val idList = ids.toList
        val q      = quote(customerTable.filter(c => liftQuery(idList).contains(c.id)))
        ctx.runQuery(run(q)).flatMap(ZIO.foreach(_)(CustomerPEConverter.toCustomer))
      }
}

object CustomerRepoMySQLImpl {
  val layer: URLayer[SqlContext & Transactor, CustomerRepo] =
    ZLayer.fromFunction(new CustomerRepoMySQLImpl(_, _))
}
