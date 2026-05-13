# Cross-Context Calls

Practiced disciplines for one bounded context calling another. Builds on [`ctx-api.md`](ctx-api.md) (trait + TO mechanics) — this doc covers where the calls live, how the api modules stay decoupled, and how failures behave.

The example to read against this doc: `NotificationAppService` calls `CustomerApi` in two places — an existence check on `create`, and a batch lookup for `get`/`list` enrichment.

## Where the cross-context call lives

**App service, not the route.** Cross-context calls belong in the calling context's `<Name>AppService` — the layer whose purpose is *composing operations across the ctx's own concerns and its external collaborators*. The route stays a thin shell: parse inbound, call app-service, render outbound.

Two consequences of this discipline:

- The route's signature stays free of `<Foreign>Api`. Adding a new endpoint that needs the same cross-ctx data doesn't introduce a second cross-ctx call site.
- The app-service's signature is honest about every collaborator. `NotificationAppServiceImpl(notificationService, customerApi)` declares both ctxs at the constructor; reading the constructor tells you the full local frame. See [`local-reasoning.md`](local-reasoning.md).

The alternative — calling the cross-ctx api from the route — pushes domain-rule logic ("can't create a notification without a recipient") into the presentation layer. Existence checks would then be enforced by *whoever happens to call the route*; an internal caller bypassing the route would bypass the check. Domain rules belong in the domain layer's orchestrator.

## Decouple the api modules from each other

`<ctx>-api` modules are **leaves** in the dependency graph. They depend on `libCommon` and nothing else — not on each other. The reason is co-evolution: any module that imports `<foreign>-api` is shaped by the foreign ctx's wire format. If contracts cross-reference each other, you can't evolve one without considering callers of the other.

This rules out a tempting shape: `NotificationWithRecipientTO(notification: NotificationTO, recipient: CustomerTO)`. Embedding `CustomerTO` would make `notification-api` transitively expose customer-api's wire format. Instead, notification defines its own `NotificationRecipientTO` — a projection of what notification cares about (id, name, email). The boundary mapping `CustomerTO → NotificationRecipientTO` lives in `notification/impl/` (the only place that sees both sides).

The same pattern applies on the domain side. `NotificationRecipient` is a notification-domain type, not an import of `Customer`. Customer's domain types stay private to customer; notification reasons about its own representation of "who this notification is for."

## What gets exposed in `<ctx>-api`

Only what callers actually call. `CustomerApi` doesn't expose `list` because notification doesn't list customers. It exposes `getMany` because notification needs batch lookup for its list-enrichment path.

The corollary: when a caller no longer uses a method, prune it. Callers of `<ctx>-api` are visible in the build graph (whoever does `dependsOn(ctxFooApi)`); a method nobody uses is dead surface area.

## Failure handling: pass-through by default

When `CustomerApi.get(recipientId)` raises `CustomerNotFoundError` and notification is in the middle of creating a record, two strategies are available:

- **Pass-through** (what we do): the foreign error flows through `NotificationAppService.create` unchanged into the HTTP layer, which renders 404.
- **Re-map**: notification catches and maps to a notification-scoped error like `RecipientNotFoundError`.

The codebase defaults to pass-through. Reasons:

1. **Honest error narrative.** A response saying "customer not found" with the recipient id is a more accurate story than "notification create failed because of an unknown recipient" with the same id. Less translation, less loss of detail.
2. **Mapping costs surface area.** Each re-mapping is a new error variant + a `mapError` site. Without a domain reason for the mapping, it's bookkeeping.

Re-mapping is the right call when notification needs to *enrich* the error context — e.g. "customer X not found while creating notification N" with both ids in the message. In that case the new variant is doing semantic work, not just renaming.

## Batching and N+1

A list endpoint that fetches a related entity per row is N+1 by default — once for each row in the parent set. The fix lives in the api trait: expose a batch op that takes a `Set[Id]` and returns a `Map[Id, T]`.

`CustomerApi.getMany(ids: Set[CustomerId]): AppIO[Map[CustomerId, CustomerTO]]` is shaped for the consumer. The returned map's key set is a subset of the input — ids that don't resolve are absent. Callers decide what absence means.

## What absence means depends on the caller

Notification's list path calls `getMany` with the recipient ids of the loaded notifications. A recipient missing from the result indicates that a notification references a customer the customer ctx can't produce. The customer ctx has no delete operation in this codebase, so the implied invariant ("every `notification.recipient_id` resolves") matches what the schema guarantees. A miss means data drift — surfaced as `OrphanedRecipientError` (`reason: OrphanedRecipient`, rendered 500). The error variant lives in `notification.domain.error`, not as a generic backend error, so ops can alert on the named reason.

When a delete operation is added later, this is where the design choice surfaces. The migration's current FK is `ON DELETE CASCADE` — a hard customer-delete would take notification rows with it, so the read path never sees the dangling reference. That's convenient *and* a schema-level cross-ctx coupling: customer's `DELETE` reaches into notification's table without going through `NotificationApi`, exactly the kind of inter-ctx write this doc argues against at the application layer. Soft-delete with a visibility filter would want different behavior: either `recipient` becomes `Option` (the absent state enters the legal-state space) or notification chooses a deliberate fallback. The right call depends on the soft-delete semantics, so the choice can wait.

## Microservice extraction

The disciplines above keep call sites unchanged when a foreign ctx moves to a separate deployment — only the wired impl swaps. Owner: [`ctx-api.md`](ctx-api.md#microservice-extraction-path).
