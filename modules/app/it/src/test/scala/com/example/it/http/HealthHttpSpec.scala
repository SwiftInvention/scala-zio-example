package com.example.it.http

import com.example.common.test.IntegrationSpec
import zio._
import zio.http._
import zio.test.Assertion._
import zio.test._

object HealthHttpSpec extends IntegrationSpec {

  override def spec: Spec[Any, Throwable] = suite("Health probes")(
    test("GET /health returns 200 (liveness — independent of DB)") {
      (for {
        ts <- ZIO.service[TestServer]
        r  <- ts.get("/health")
      } yield assert(r.status)(equalTo(Status.Ok))).provide(TestServer.layer)
    },
    test("GET /ready returns 200 when the DB is reachable") {
      (for {
        ts <- ZIO.service[TestServer]
        r  <- ts.get("/ready")
      } yield assert(r.status)(equalTo(Status.Ok))).provide(TestServer.layer)
    },
    test("GET /docs serves the Swagger UI HTML, GET /docs/<title>.json returns the spec") {
      (for {
        ts       <- ZIO.service[TestServer]
        ui       <- ts.get("/docs")
        uiBody   <- ui.body.asString
        spec     <- ts.get("/docs/scala-zio-example.json")
        specBody <- spec.body.asString
      } yield assert(ui.status)(equalTo(Status.Ok)) &&
        assert(uiBody)(containsString("swagger-ui")) &&
        assert(spec.status)(equalTo(Status.Ok)) &&
        assert(specBody)(containsString("\"openapi\"")) &&
        assert(specBody)(containsString("/customers"))).provide(TestServer.layer)
    }
  )
}
