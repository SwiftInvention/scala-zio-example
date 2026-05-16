# TO Converters

Each entity has a converter object that translates between its domain form and its wire (TO) form. One file per entity, hand-written, in the ctx's `impl/`.

## Where they live

```text
modules/ctx/<name>/src/main/scala/com/example/<name>/impl/to/converter/
└── <Entity>Converter.scala
```

`impl/to/` is the impl-side home for TO-related code. It's the only place that can see both sides — the domain entity (its own module) and the TO (`<name>-api`).

The converter is also the place where smart-constructed domain types (`Email`, `CustomerName`) flatten to wire-side primitives — domain types stay at the domain, TOs serve the wire's own constraints.

## Shape

```scala
package com.example.ctx.customer.impl.to.converter

import com.example.ctx.customer.api.to.CustomerTO
import com.example.ctx.customer.domain.model.Customer

object CustomerConverter {
  def toCustomerTO(d: Customer): CustomerTO =
    CustomerTO(d.id, d.email, d.name)

  def toCustomer(to: CustomerTO): Customer =
    Customer(to.id, to.email, to.name)
}
```

Method names are entity-qualified: `to<Entity>TO` for domain → TO, `to<Entity>` for TO → domain. For create-command shapes: `toNew<Entity>(to: Create<Entity>RequestTO): New<Entity>`.

## At call sites

Either qualified or imported:

```scala
import com.example.ctx.customer.impl.to.converter.CustomerConverter._

appService.list.map(_.map(toCustomerTO))                // imported
appService.list.map(_.map(CustomerConverter.toCustomerTO))  // qualified
```

Imported form keeps map bodies clean. Qualified form is unambiguous when multiple converters are in scope at one call site.

## One file per entity

Each entity gets its own `<Entity>Converter.scala`. Tightly-paired sub-entities that always appear together can share a file (e.g. `CustomerConverter` also handling `Address`).
