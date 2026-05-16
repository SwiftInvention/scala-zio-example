# Bounded Context

A bounded context is implemented in `ctx/<name>/`. When another context needs to call in, a sibling `ctx/<name>-api/` module appears holding the cross-context trait and the TOs that cross the boundary.

```text
ctx/<name>/       implementation (always present)
ctx/<name>-api/   cross-context contract (added when a foreign ctx imports it)
```

A ctx without a cross-context caller has no `-api` module. The ctx's HTTP wire format lives in `ctx/<name>/impl/to/` — that's a separate concern from cross-context wire format (see [`cross-context-call.md`](cross-context-call.md) §"The api module").

## Internal layout of `ctx/<name>/`

```text
modules/ctx/<name>/src/main/scala/com/example/ctx/<name>/
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
    ├── <Name>ApiDirectImpl.scala    bridge to <name>-api contract (only when -api exists)
    ├── service/                     mirrors domain/service/
    │   ├── <Name>ServiceImpl.scala
    │   └── repo/
    │       ├── <Name>RepoMySQLImpl.scala  queries PE from lib/db, converts via PEConverter
    │       └── converter/
    │           └── <Name>PEConverter.scala
    ├── to/                          HTTP wire format
    │   ├── <Name>TO.scala           TOs that only the ctx's own HTTP routes use
    │   └── converter/
    │       └── <Name>Converter.scala
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

`<Name>AppService` is the fan-out point: the cross-context bridge (`<Name>ApiDirectImpl`) and the HTTP routes are sibling consumers, not chained. The cross-context branch exists only when another ctx imports `<name>-api`; without a consumer, neither the trait nor the DirectImpl exist. See [`cross-context-call.md`](cross-context-call.md) §"Routes don't go through the api" for why the HTTP plane bypasses `<Name>Api`.

Domain services host validation and business rules; AppService orchestrates them. If `<Name>Service` would be pure delegation, drop it and have AppService call Repo directly.

## Why `app/` colocates trait and impl

Service and Repo impls live in `impl/` because they may have multiple variants — `Stub`, `MySQL`, `Postgres` — chosen at composition time. AppService is different: a thin orchestrator tightly bound to its trait, almost always one impl, and read as a trait+impl pair.

## Import shape

The `impl → app → domain` convention applies inside a context, the same way it applies across modules. See [`build-deps.md`](build-deps.md) §"Convention-only: `impl → app → domain` import direction".
