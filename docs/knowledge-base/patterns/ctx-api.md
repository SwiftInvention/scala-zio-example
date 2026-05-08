# Cross-Context API

Each context publishes a TO-typed trait in its `-api` module. That's the surface other contexts call into; nothing else from the ctx is reachable without going through it.

## Contents of `ctx/<name>-api/`

```
modules/ctx/<name>-api/src/main/scala/com/example/<name>/api/
├── <Name>Api.scala       the trait (TO-typed signatures)
└── to/                   transfer objects (wire format)
    └── <Name>TO.scala
```

That's it. No errors, no domain logic.

## The DirectClient impl

The trait's implementation lives in the ctx module at `impl/<Name>ApiDirectImpl.scala`:

```scala
final class CustomerApiDirectImpl(appService: CustomerAppService) extends CustomerApi {
  override def list: AppIO[List[CustomerTO]] =
    appService.list.map(_.map(toTO))

  private def toTO(d: Customer): CustomerTO =
    CustomerTO(d.id, d.email, d.name)
}

object CustomerApiDirectImpl {
  val layer: URLayer[CustomerAppService, CustomerApi] =
    ZLayer.fromFunction(new CustomerApiDirectImpl(_))
}
```

It takes `<Name>AppService` (domain-typed) as a constructor dep and maps domain entities to TOs at the boundary.

## Where TO ↔ domain conversion lives, and why

The conversion happens in `<Name>ApiDirectImpl`, inside the ctx module. The reason is forced by the dependency graph: the mapping needs to see both sides — the domain entity (in the ctx module) and the TO (in `<name>-api`) — and the ctx module is the only place that compiles with both visible. Reversing the dependency, so `<name>-api` knew about the ctx's internals, would let every module depending on `<name>-api` transitively see those internals. That defeats the split.

## Relationship to HTTP routes

Routes (in `<ctx>/impl/http/`) talk to `<Name>AppService` directly, not through `<Name>Api`. The frontend wire format and the cross-context wire format are separate concerns: a frontend may want richer payloads than what other contexts need.

A consequence: `<Name>AppService` is typically wider than `<Name>Api`. The cross-context trait only contains operations actually called from other contexts. Operations that exist purely to serve this context's own routes stay on AppService and never appear in the api trait.

The cross-context API is for *intra-system* calls (between contexts in the same JVM, monolith mode). External clients hit the HTTP routes.

## Microservice extraction path

When a context becomes its own service:

1. Add `<Name>ApiHttpImpl` alongside `<Name>ApiDirectImpl` (same `impl/` folder). Same trait, marshals over HTTP.
2. The composition root swaps in the HTTP impl instead of the Direct impl. Other modules don't change.
3. At true extraction time — caller deployed separately, can't see the ctx module on its classpath — move `<Name>ApiHttpImpl` to a new `<name>-client` module that depends only on `<name>-api`. The caller deployment includes `<name>-api` + `<name>-client`. The customer service deployment includes `<name>` and uses the Direct impl internally for any same-JVM callers.

The migration is per-context and localized. Don't pre-pay the structure cost (`-client` module) before extraction is on the table.
