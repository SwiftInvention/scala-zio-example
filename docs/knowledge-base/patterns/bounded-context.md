# Bounded Context

A bounded context is a pair of sbt modules — a public contract (`<name>-api`) and an implementation (`<name>`). Other contexts depend only on the contract; the composition root supplies the impl.

```text
ctx/<name>-api/   cross-context contract (trait + TOs)
ctx/<name>/       implementation
```

See [`cross-context-call.md`](cross-context-call.md) §"The api module: `ctx/<name>-api/`" for what lives in the `-api` module.

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
    │       ├── <Name>RepoMySQLImpl.scala  queries PE from lib/db, converts via PEConverter
    │       └── converter/
    │           └── <Name>PEConverter.scala
    └── http/
        ├── <Name>Endpoints.scala    typed endpoint definitions (wire shape, no behavior)
        └── <Name>Routes.scala       implementations against <Name>Endpoints
```

`impl/service/` mirrors `domain/service/` so finding a trait's impl is a mechanical translation. PEs and DbSchema traits live in `lib/db` (one schema for the whole deployment — see [`persistence.md`](persistence.md)); the ctx ships the repo impl that queries them and the PE↔domain converter. See [`http-endpoints.md`](http-endpoints.md) for how `<Name>Endpoints` definitions feed both the routes and the OpenAPI document.

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

`<Name>AppService` is the fan-out point: the cross-context bridge (`<Name>ApiDirectImpl`) and the HTTP routes are sibling consumers, not chained. See [`cross-context-call.md`](cross-context-call.md) §"Routes don't go through the api" for why the HTTP plane bypasses `<Name>Api`.

Each link is a thin pass-through unless it has something to add. Domain services host validation and business rules; AppService orchestrates them. If `<Name>Service` would be pure delegation, drop it and have AppService call Repo directly.

## Why `app/` colocates trait and impl

Service and Repo impls live in `impl/` because they may have multiple variants — `Stub`, `MySQL`, `Postgres` — chosen at composition time. AppService is different: a thin orchestrator tightly bound to its trait, almost always one impl, and read as a trait+impl pair. Colocating them keeps that cohesion visible.

## Import shape

The `impl → app → domain` convention applies inside a context, the same way it applies across modules. Owner: [`build-deps.md`](build-deps.md) §"Convention-only: `impl → app → domain` import direction".
