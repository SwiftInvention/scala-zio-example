# Bounded Context

A bounded context is a pair of sbt modules — a public contract (`<name>-api`) and an implementation (`<name>`). Other contexts depend only on the contract; the composition root supplies the impl.

```text
ctx/<name>-api/   cross-context contract (trait + TOs)
ctx/<name>/       implementation
```

See [`ctx-api.md`](ctx-api.md) for what lives in the `-api` module.

## Internal layout of `ctx/<name>/`

```text
modules/ctx/<name>/src/main/scala/com/example/<name>/
├── domain/                          abstractions only — no concrete impls
│   ├── error/                       domain errors
│   ├── model/                       entities, value types
│   └── service/
│       ├── <Name>Service.scala      domain service trait
│       └── repo/
│           └── <Name>Repo.scala     repo trait
├── app/                             application service (special: trait + impl colocated)
│   ├── <Name>AppService.scala
│   └── <Name>AppServiceImpl.scala
└── impl/                            concrete implementations
    ├── <Name>ApiDirectImpl.scala    bridge to <name>-api contract
    ├── service/                     mirrors domain/service/
    │   ├── <Name>ServiceImpl.scala
    │   └── repo/
    │       └── <Name>RepoImpl.scala
    └── http/
        ├── <Name>Endpoints.scala    typed endpoint definitions (wire shape, no behavior)
        └── <Name>Routes.scala       implementations against <Name>Endpoints
```

`impl/service/` mirrors `domain/service/` so finding a trait's impl is a mechanical translation. See [`http-endpoints.md`](http-endpoints.md) for how `<Name>Endpoints` definitions feed both the routes and the OpenAPI document.

## Layer chain

```text
<Name>RepoImpl.layer           provides <Name>Repo, no project deps
   ↓
<Name>ServiceImpl.layer        takes <Name>Repo, provides <Name>Service
   ↓
<Name>AppServiceImpl.layer     takes <Name>Service, provides <Name>AppService
   ├─→ <Name>ApiDirectImpl.layer   takes <Name>AppService, provides <Name>Api    (cross-context plane)
   └─→ <Name>Routes.layer          takes <Name>AppService, exposes Routes[Any, Response]   (HTTP plane)
```

`<Name>AppService` is the fan-out point: the cross-context bridge (`<Name>ApiDirectImpl`) and the HTTP routes are sibling consumers, not chained. See [`ctx-api.md`](ctx-api.md#relationship-to-http-routes) for why the HTTP plane bypasses `<Name>Api`.

Each link is a thin pass-through unless it has something to add. Domain services host validation and business rules; AppService orchestrates them. When a link has nothing to add, drop it — AppService can call Repo directly when there's no domain logic worth a Service layer, and `<Name>Service` doesn't need to exist if it would be pure delegation.

## Why `app/` colocates trait and impl

Service and Repo impls live in `impl/` because they may have multiple variants — `Stub`, `MySQL`, `Postgres` — chosen at composition time. AppService is different: a thin orchestrator tightly bound to its trait, almost always one impl, and read as a trait+impl pair. Colocating them keeps that cohesion visible.

## Import shape

The `impl → app → domain` convention applies inside a context, the same way it applies across modules. Owner: [`build-deps.md`](build-deps.md) §"Convention-only: `impl → app → domain` import direction".
