package org.organization.http

import io.circe.generic.auto._
import org.organization.AppEnv.AppEnv
import org.organization.api.model.NewType.PersonIdentifier
import org.organization.api.to.{NewPersonTO, PersonTO}
import org.organization.db.repository.PersonRepository
import org.organization.http.BaseEndpoint.{makeEndpoint, makeEndpointHandler}
import sttp.tapir._
import sttp.tapir.codec.newtype._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.ztapir.ZTapir
import zio.ZIO

object PersonEndpoint extends PersonRepository with ZTapir with TapirCodecNewType {

  val personList: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "Default-endpoint",
        "Get persons from database"
      ).get
        .in("person" / "list")
        .out(jsonBody[List[PersonTO]])
    )(_ => getPersons.map(_.map(_.toTO)).mapError(_ => InternalServerError))

  val allPersonList: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "get-all-person",
        "Get persons from database, including archived"
      ).get
        .in("person" / "list" / "all")
        .out(jsonBody[List[PersonTO]])
    )(_ => getAllPersons.map(_.map(_.toTO)).mapError(_ => InternalServerError))

  val personByIdentifier: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "get-person-by-identifier",
        "Get person from database by identifier"
      ).get
        .in("person" / path[PersonIdentifier]("identifier"))
        .out(jsonBody[PersonTO])
    )(identifier =>
      getByIdentifier(identifier)
        .foldZIO(
          _ => ZIO.fail(InternalServerError),
          {
            case None         => ZIO.fail(NotFound)
            case Some(person) => ZIO.succeed(person.toTO)
          }
        )
    )

  val oldestPerson: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "get-oldest-person",
        "Get the oldest person from database"
      ).get
        .in("person" / "oldest")
        .out(jsonBody[PersonTO])
    )(_ =>
      getOldest
        .foldZIO(
          _ => ZIO.fail(InternalServerError),
          {
            case None         => ZIO.fail(NotFound)
            case Some(person) => ZIO.succeed(person.toTO)
          }
        )
    )

  val createPerson: ZServerEndpoint[AppEnv, Any] =
    makeEndpointHandler(
      makeEndpoint(
        "create-person",
        "Add new person to database"
      ).post
        .in("person" / jsonBody[NewPersonTO])
    )(newPersonTO => insert(newPersonTO.toDomain()).unit.orElseFail(InternalServerError))

}
