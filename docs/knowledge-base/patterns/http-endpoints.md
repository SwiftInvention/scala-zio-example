# HTTP Endpoints

Routes are defined via zio-http's typed `Endpoint` API: each route's wire shape (path pattern, input types, output type, possible error responses with their HTTP statuses) is a value of type `Endpoint[...]` separate from its implementation. The same value feeds two consumers:

- The running server, via `endpoint.implement { input => effect }.toRoutes`.
- The OpenAPI document, via `OpenAPIGen.fromEndpoints(...)` aggregated in `ServerApp`.

## File layout per ctx

```text
<ctx>/impl/http/
├── <Name>Endpoints.scala    pure Endpoint values + an `all` aggregator
└── <Name>Routes.scala       implementations against the endpoints
```

`<Name>Endpoints.scala` exports `val all: List[Endpoint[...]]` — the aggregator that `ServerApp` collects for OpenAPI generation. New endpoints get added to `all` at the same time they're defined.

Operational endpoints live in `lib/common/impl/http/server/` — same split: `HealthEndpoints` + `HealthRoutes`. The typed-Endpoint wire format (`ApiFailure`) and shared middleware (`RequestLogging`, `RequestTracing`) live next to them.

## Error model: `ApiFailure`

zio-http's Endpoint API binds HTTP status to error type at definition time — different statuses require different value types. The codebase reconciles this with the `AppFailure` family via a small wire-format ADT in `lib/common/impl/http/ApiFailure.scala`:

```scala
sealed trait ApiFailure { def body: ErrorTO }
object ApiFailure {
  final case class NotFoundResponse(body: ErrorTO)   extends ApiFailure
  final case class BadRequestResponse(body: ErrorTO) extends ApiFailure
  final case class InternalResponse(body: ErrorTO)   extends ApiFailure
  // ... one variant per HTTP status family
}
```

Each variant's `Schema` transforms to `ErrorTO`'s flat shape, so the JSON body on the wire is `{"code": ..., "category": ..., "reason": ..., "description": ...}` regardless of status.

Application endpoints declare the full `ApiFailure` variant set via `outErrors`:

```scala
import ApiFailure.{badRequestCodec, unauthorizedCodec, forbiddenCodec,
                   notFoundCodec, internalCodec, serviceUnavailableCodec}

val get = Endpoint(Method.GET / "customers" / PathCodec.string("id"))
  .out[CustomerTO]
  .outErrors[ApiFailure](
    badRequestCodec, unauthorizedCodec, forbiddenCodec,
    notFoundCodec, internalCodec, serviceUnavailableCodec
  )
```

Codec vals live in `ApiFailure` (one per variant) and are imported by every `<Name>Endpoints.scala`. The impl's error type is uniformly `ApiFailure` (the trait); routes use `mapError(ApiFailure.from)`. `ApiFailure.from` matches on `f.asHttpError`, the self-typed `HttpError` view — exhaustive over `HttpError`'s sealed sub-traits, so the build breaks if a new `HttpError` trait lands without a matching `ApiFailure` variant. The framework picks the matching codec by `ClassTag` at serialize time.

The trade-off: every endpoint's OpenAPI documents all 6 status families even when the impl only produces a subset — the cost of guaranteeing the framework always has a codec. Declaring a per-endpoint subset is unsafe: under impl drift, zio-http renders an undeclared variant as `HTTP 400` with body `{"name":"EncodingResponseError",...}`.

Adding a new `HttpError` trait requires four coordinated changes: a new variant in `ApiFailure`, a new codec val, a new branch in `ApiFailure.from` (the build will break here if you forget), and a new entry in each `<Name>Endpoints.scala`'s `outErrors` arg list.

### Force-wrapping is for probe-style endpoints only

The readiness probe (`/ready`) declares `outError[ApiFailure.ServiceUnavailableResponse]` (single variant) and force-wraps any `AppFailure` as that variant — the probe's contract is the translation (any cause → 503, by definition of "not ready"). Avoid this pattern for application endpoints, where impl evolution would silently demote typed statuses.

## Adding a new endpoint

1. Add an `Endpoint` value to the relevant `<Name>Endpoints.scala`.
2. Add it to `<Name>Endpoints.all`.
3. Add an `endpoint.implement { input => effect }` block to the corresponding `<Name>Routes.scala`, append `.toRoutes` to its `routes` aggregation.
4. The new endpoint shows up in `/docs` automatically — no separate registration in `ServerApp`.

## Why typed Endpoints

The classic `Routes(Method.GET / "..." -> handler {...})` API is more compact for one-off routes but doesn't carry the wire shape as a value. The Endpoint API trade-off:

- More boilerplate per route (definition + implementation, two sites instead of one).
- Wire shape is a value, reusable for OpenAPI generation, future client generation, contract testing.
- Path parameter types are checked at the boundary (`PathCodec.string("id")` produces a `String` input to the handler).
- Error model is explicit at the type level — the handler effect's error type matches the endpoint's declared error.
