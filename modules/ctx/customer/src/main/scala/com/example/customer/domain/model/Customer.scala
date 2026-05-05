package com.example.customer.domain.model

import com.example.common.domain.model.NewTypes.CustomerId

final case class Customer(
    id: CustomerId,
    email: Email,
    name: CustomerName
)
