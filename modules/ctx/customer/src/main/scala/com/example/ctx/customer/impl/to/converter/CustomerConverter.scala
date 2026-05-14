package com.example.ctx.customer.impl.to.converter

import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.customer.domain.model.Customer
import com.example.lib.common.domain.model.Types.AppIO
import com.example.lib.common.domain.model.{CustomerName, Email}

/** TO ↔ domain mapping for `Customer`. */
object CustomerConverter {

  def toCustomerTO(d: Customer): CustomerTO =
    CustomerTO(id = d.id, email = d.email.value, name = d.name.value)

  def toCustomer(to: CustomerTO): AppIO[Customer] =
    for {
      email <- Email(to.email)
      name  <- CustomerName(to.name)
    } yield Customer(id = to.id, email = email, name = name)
}
