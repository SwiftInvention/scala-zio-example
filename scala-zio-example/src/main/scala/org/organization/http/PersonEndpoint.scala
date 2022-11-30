package org.organization.http

import org.organization.db.model.Person
import org.organization.db.repository.PersonRepository
import org.organization.AppEnv.AppEnv
import sttp.tapir._
import io.circe.generic.auto._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir.ZTapir

object PersonEndpoint extends PersonRepository with ZTapir {

  val personListing: PublicEndpoint[Unit, Unit, List[Person], Any] =
    endpoint.get
      .name("Default-endpoint")
      .description("Get all persons from database")
      .in("person" / "list" / "all")
      .out(jsonBody[List[Person]])

  // TODO: Provide error body, not just Unit
  val personListingServerLogic: ZServerEndpoint[AppEnv, Any] =
    personListing.zServerLogic(_ => getAllPersons.mapError(_ => ()))

}
