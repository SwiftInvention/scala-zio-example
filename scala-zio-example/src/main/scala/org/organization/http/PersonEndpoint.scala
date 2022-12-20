package org.organization.http

import io.circe.generic.auto._
import org.organization.AppEnv.AppEnv
import org.organization.api.to.{NewPersonTO, PersonTO}
import org.organization.db.repository.PersonRepository
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir.ZTapir
import zio.ZIO

import java.util.UUID

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

  private val personByIdentifier: PublicEndpoint[UUID, Unit, PersonTO, Any] =
    endpoint.get
      .name("get-person-by-identifier")
      .description("Get person from database by identifier")
      .in("person" / path[UUID]("personUUID"))
      .out(jsonBody[PersonTO])

  // TODO: Provide error body, not just Unit
  val personByIdentifierServerLogic: ZServerEndpoint[AppEnv, Any] = {
    personByIdentifier.zServerLogic(personUUID =>
      getByIdentifier(personUUID)
        .mapError(_ => ())
        .flatMap({
          case None         => ZIO.fail(())
          case Some(person) => ZIO.succeed(person.toTO())
        })
    )
  }

  private val createPerson: PublicEndpoint[NewPersonTO, Unit, Unit, Any] =
    endpoint.post
      .name("create-person")
      .description("Add new person to database")
      .in("person")
      .in(jsonBody[NewPersonTO])

  // TODO: Provide error body, not just Unit
  val addPersonServerLogic: ZServerEndpoint[AppEnv, Any] = {
    createPerson.zServerLogic(newPersonTO =>
      insert(newPersonTO.toEnt()).map(_ => ()).mapError(_ => ())
    )
  }

}
