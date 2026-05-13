# Cross-Context Calls

How one bounded context calls another: the surface (`<ctx>-api`), where the call site lives, how the api modules stay decoupled, how failures behave, and how the discipline survives a microservice extraction.

The example to read against this doc: `NotificationAppService` calls `CustomerApi` in two places — an existence check on `create`, and a batch lookup for `get`/`list` enrichment.

## The api module: `ctx/<name>-api/`

```text
modules/ctx/<name>-api/src/main/scala/com/example/<name>/api/
├── <Name>Api.scala       the trait (TO-typed signatures)
└── to/                   transfer objects (wire format)
    └── <Name>TO.scala
```

Only the trait and its TOs. No errors, no domain logic — those live in the impl ctx.

`<ctx>-api` modules are **leaves** in the build graph. They depend on `libCommon` and nothing else — including no other `-api` module. The codebase-wide rule lives in [`build-deps.md`](build-deps.md); the reason it matters here is co-evolution: any module that imports `<foreign>-api` is shaped by the foreign ctx's wire format. If contracts cross-reference each other, you can't evolve one without considering callers of the other.

This rules out a tempting shape: `NotificationWithRecipientTO(notification: NotificationTO, recipient: CustomerTO)`. Embedding `CustomerTO` would make `notification-api` transitively expose customer-api's wire format. Instead, notification defines its own `NotificationRecipientTO` — a projection of what notification cares about (id, name, email). The boundary mapping `CustomerTO → NotificationRecipientTO` lives in `notification/impl/` (the only place that sees both sides).

The same pattern applies on the domain side. `NotificationRecipient` is a notification-domain type, not an import of `Customer`. Customer's domain types stay private to customer; notification reasons about its own representation of "who this notification is for."

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

It takes `<Name>AppService` (domain-typed) as a constructor dep and maps domain entities to TOs at the boundary. The ctx module is the only place that compiles with both the domain entity and the TO visible — owner of the converter story: [`converters.md`](converters.md).

## What gets exposed in `<ctx>-api`

Only what callers actually call. `CustomerApi` doesn't expose `list` because notification doesn't list customers. It exposes `getMany` because notification needs batch lookup for its list-enrichment path.

Corollary: when a caller no longer uses a method, prune it. Callers of `<ctx>-api` are visible in the build graph (whoever does `dependsOn(ctxFooApi)`); a method nobody uses is dead surface area.

## Routes don't go through the api

Routes (in `<ctx>/impl/http/`) talk to `<Name>AppService` directly, not through `<Name>Api`. The frontend wire format and the cross-context wire format are separate concerns — a frontend may want richer payloads than what other contexts need. `<Name>AppService` is typically wider than `<Name>Api`; the cross-context trait only contains operations actually called from other contexts.

The cross-context API is for *intra-system* calls (between contexts in the same JVM). External clients hit the HTTP routes.

## Where the cross-context call lives

**App service, not the route.** Cross-context calls belong in the calling context's `<Name>AppService`. The route stays a thin shell: parse inbound, call app-service, render outbound.

Two consequences:

- The route's signature stays free of `<Foreign>Api`. A second endpoint needing the same cross-ctx data doesn't introduce a second call site.
- The app-service's signature is honest about every collaborator. `NotificationAppServiceImpl(notificationService, customerApi)` declares both ctxs at the constructor; reading the constructor tells you the full local frame ([`local-reasoning.md`](local-reasoning.md) §"Explicit dependencies").

## Failure handling: pass-through by default

When `CustomerApi.get(recipientId)` raises `CustomerNotFoundError` and notification is in the middle of creating a record, two strategies are available:

- **Pass-through** (default): the foreign error flows through `NotificationAppService.create` unchanged into the HTTP layer, which renders 404.
- **Re-map**: notification catches and maps to a notification-scoped error like `RecipientNotFoundError`.

Pass-through wins by default because a response saying "customer not found" with the recipient id is a more accurate story than "notification create failed because of an unknown recipient" with the same id, and because each re-mapping is a new error variant + a `mapError` site without doing semantic work.

Re-mapping is the right call when notification needs to *enrich* the error context — e.g. "customer X not found while creating notification N" with both ids in the message. The new variant earns its place by carrying information the foreign one couldn't.

## Batching and N+1

A list endpoint that fetches a related entity per row is N+1 by default. The fix lives in the api trait: expose a batch op that takes a `Set[Id]` and returns a `Map[Id, T]`.

`CustomerApi.getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, CustomerTO]]` is shaped for the consumer. The returned map's key set is a subset of the input — ids that don't resolve are absent. Callers decide what absence means.

## What absence means depends on the caller

Notification's list path calls `getMany` with the recipient ids of the loaded notifications. A recipient missing from the result indicates a notification references a customer the customer ctx can't produce. The customer ctx has no delete operation in this codebase, so the implied invariant ("every `notification.recipient_id` resolves") matches what the schema guarantees. A miss means data drift — surfaced as `OrphanedRecipientError` (`reason: OrphanedRecipient`, rendered 500). The error variant lives in `notification.domain.error`, not as a generic backend error, so ops can alert on the named reason.

## Microservice extraction path

The disciplines above keep call sites unchanged when a foreign ctx moves to a separate deployment — only the wired impl swaps. The migration:

1. Add `<Name>ApiHttpImpl` alongside `<Name>ApiDirectImpl` (same `impl/` folder). Same trait, marshals over HTTP.
2. The composition root swaps in the HTTP impl instead of the Direct impl. Other modules don't change.
3. At true extraction time — caller deployed separately, can't see the ctx module on its classpath — move `<Name>ApiHttpImpl` to a new `<name>-client` module that depends only on `<name>-api`. The caller deployment includes `<name>-api` + `<name>-client`. The owning service deployment includes `<name>` and uses the Direct impl internally for any same-JVM callers.

Per-context and localized. Don't pre-pay the `-client` module structure before extraction is on the table.
