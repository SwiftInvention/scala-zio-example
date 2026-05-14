package com.example.ctx.customer.domain.model

import com.example.lib.common.domain.model.NewTypes.CustomerId
import com.example.lib.common.domain.model.{CustomerName, Email}

final case class Customer(
    id: CustomerId,
    email: Email,
    name: CustomerName
)
