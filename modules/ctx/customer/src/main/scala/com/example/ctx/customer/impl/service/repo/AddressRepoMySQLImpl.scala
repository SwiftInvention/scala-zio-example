package com.example.ctx.customer.impl.service.repo

import com.example.ctx.customer.domain.model.Address
import com.example.ctx.customer.domain.service.repo.AddressRepo
import com.example.ctx.customer.impl.service.repo.converter.AddressPEConverter
import com.example.lib.common.domain.model.NewTypes.{AddressId, CustomerId}
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.db.domain.service.Transactor
import com.example.lib.db.impl.repo.sql.SqlContext
import com.example.lib.db.impl.repo.sql.schema.AddressDbSchema
import zio._

final class AddressRepoMySQLImpl(val ctx: SqlContext, transactor: Transactor) extends AddressRepo with AddressDbSchema {
  import ctx.extras._
  import ctx._

  override def find(id: AddressId): AppIO[Option[Address]] =
    transactor.withTransaction {
      val q = quote(addressTable.filter(_.id === lift(id)))
      ctx.runQuerySingleResult(run(q)).flatMap {
        case Some(pe) => AddressPEConverter.toAddress(pe).map(Some(_))
        case None     => ZIO.none
      }
    }

  override def listForCustomer(customerId: CustomerId): AppIO[List[Address]] =
    transactor.withTransaction {
      val q = quote(addressTable.filter(_.customerId === lift(customerId)))
      ctx.runQuery(run(q)).flatMap(ZIO.foreach(_)(AddressPEConverter.toAddress))
    }
}

object AddressRepoMySQLImpl {
  val layer: URLayer[SqlContext & Transactor, AddressRepo] =
    ZLayer.fromFunction(new AddressRepoMySQLImpl(_, _))
}
