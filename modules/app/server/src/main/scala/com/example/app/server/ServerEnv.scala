package com.example.app.server

import com.example.customer.app.CustomerAppServiceImpl
import com.example.customer.impl.CustomerApiDirectImpl
import com.example.customer.impl.http.CustomerRoutes
import com.example.customer.impl.service.CustomerServiceImpl
import com.example.customer.impl.service.repo.CustomerRepoImpl
import zio._
import zio.http.Server

/** Layer composition for the server app. The single place that sees concrete
  * implementations and wires them together.
  */
object ServerEnv {
  type AppEnv = CustomerRoutes & Server

  val layer: ZLayer[Any, Throwable, AppEnv] =
    ZLayer.make[AppEnv](
      CustomerRepoImpl.layer,
      CustomerServiceImpl.layer,
      CustomerAppServiceImpl.layer,
      CustomerApiDirectImpl.layer,
      CustomerRoutes.layer,
      Server.default
    )
}
