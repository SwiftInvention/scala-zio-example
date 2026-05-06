# TO Converters

Each entity has a converter object that translates between its domain form and its wire (TO) form. One file per entity, hand-written, in the ctx's `impl/`.

## Where they live

```
modules/ctx/<name>/src/main/scala/com/example/<name>/impl/to/converter/
└── <Entity>Converter.scala
```

`impl/to/` collects TO-related impl-side concerns (currently just converters; later may grow to validators, custom codecs, etc.). It's the only place that can see both sides — the domain entity (its own module) and the TO (`<name>-api`). See [`ctx-api.md`](ctx-api.md) for why.

## Shape

```scala
package com.example.customer.impl.to.converter

import com.example.customer.api.to.CustomerTO
import com.example.customer.domain.model.Customer

object CustomerConverter {
  def toCustomerTO(d: Customer): CustomerTO =
    CustomerTO(d.id, d.email, d.name)

  def toCustomer(to: CustomerTO): Customer =
    Customer(to.id, to.email, to.name)
}
```

Method names are entity-qualified: `to<Entity>TO` for domain → TO, `to<Entity>` for TO → domain. For create-command shapes: `toNew<Entity>(to: CreateXRequestTO): NewX`.

## Object, not trait mixin

Trait mixin gives unqualified method names inside the inheritor (`toCustomerTO(c)` directly), at the cost of obscuring where each method comes from and forcing every consumer to extend the trait. Object + explicit import (`import CustomerConverter._`) gives the same brevity at use sites with none of the inheritance plumbing, and `CustomerConverter.toCustomerTO` is greppable.

## Hand-written, not chimney

Every field appears in the source. Errors are immediate, no macro-expanded code to debug. chimney is the right call if a converter grows to dozens of fields with mostly-matching names — but for the typical case (single-digit fields, some renames, some type changes), hand-written is cheaper to read. We've dropped chimney from the deps until that case shows up.

## At call sites

Either qualified or imported:

```scala
import com.example.customer.impl.to.converter.CustomerConverter._

appService.list.map(_.map(toCustomerTO))                // imported
appService.list.map(_.map(CustomerConverter.toCustomerTO))  // qualified
```

Imported form keeps map bodies clean. Qualified form is unambiguous when multiple converters are in scope at one call site.

## One file per entity

As entities accrue (`Customer`, `Company`, `Facility`), each gets its own `<Entity>Converter.scala`. If a converter is dominated by a few tightly-paired sub-entities (e.g. `CompanyConverter` also handles `CompanyAddress` because they always appear together), keep them in the same file.
