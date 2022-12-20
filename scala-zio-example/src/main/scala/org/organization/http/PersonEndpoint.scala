package org.organization.http

import io.circe.generic.auto._
import org.organization.AppEnv.AppEnv
import org.organization.api.to.PersonTO
import org.organization.db.repository.PersonRepository
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir.ZTapir
import zio.ZIO

object PersonEndpoint extends PersonRepository with ZTapir {

  private val personListing: PublicEndpoint[Unit, Unit, List[PersonTO], Any] =
    endpoint.get
      .name("Default-endpoint")
      .description("Get all persons from database")
      .in("person" / "list" / "all")
      .out(jsonBody[List[PersonTO]])


  // TODO: Provide error body, not just Unit
  val personListingServerLogic: ZServerEndpoint[AppEnv, Any] =
    personListing.zServerLogic(_ => getAllPersons.map(_.map(_.toTO())).mapError(_ => ()))

  private val personById: PublicEndpoint[Long, Unit, PersonTO, Any] =
    endpoint.get
      .name("get-person")
      .description("Get person from database by id")
      .in("person" / path[Long]("personId"))
      .out(jsonBody[PersonTO])

  // TODO: Provide error body, not just Unit
  val personByIdServerLogic: ZServerEndpoint[AppEnv, Any] = {
    personById.zServerLogic(
      personId => getById(personId)
        .mapError(_ => ())
        .flatMap({
          case None => ZIO.fail(())
          case Some(person) => ZIO.succeed(person.toTO())
        })
    )
  }


}
