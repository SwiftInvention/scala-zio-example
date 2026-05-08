# Persistence

Quill + MySQL + Flyway, with explicit transaction boundaries.

## Layout

```
modules/lib/common/src/main/scala/com/example/common/
├── domain/service/
│   └── Transactor.scala                 trait — transaction boundary
└── impl/
    ├── repo/sql/
    │   ├── DataSourceConfig.scala       typed DB config (PureConfig case class + layer)
    │   ├── DataSourceLayer.scala        Hikari pool from DataSourceConfig
    │   ├── SqlContext.scala              Quill JDBC context + encodings
    │   └── NewTypeEncodings.scala       MappedEncodings for Newtype ids
    └── service/
        └── TransactorQuillImpl.scala    Transactor impl

modules/lib/common/src/main/resources/
└── db/migration/
    └── V<date>.<seq>__<name>.sql        Flyway-versioned migrations (applied out-of-process)

modules/ctx/<name>/src/main/scala/com/example/<name>/impl/service/repo/
├── <Name>RepoMySQLImpl.scala            MySQL impl (per impl-suffix rule)
└── sql/
    ├── <Name>DbSchema.scala             trait: querySchema for the table(s)
    ├── entity/
    │   └── <Name>PE.scala               case class mirroring the table
    └── converter/
        └── <Name>PEConverter.scala      PE ↔ domain mapping
```

The `sql/` package and `SqlContext` name are dialect-agnostic by design: switching MySQL for Postgres later is a one-line change in the Quill base trait the context extends, not a rename across every ctx.

## Migrations

**Location:** `lib/common/src/main/resources/db/migration/`, not `app/server`. Any future deployment unit (worker, sync-job, batch) will share the same schema; pinning migrations to one unit forces every other unit to depend on it just for the SQL files.

**Filename:** `V<date>.<seq>__<snake_case_name>.sql`. Lexicographic order ⇒ chronological order, and the date prefix avoids merge conflicts on the version counter.

**Applied out-of-process.** Migrations run via the `flyway` CLI from a justfile recipe, not from the Scala app at boot:

```sh
just db-migrate                         # local
flyway ... -locations="filesystem:..."  # CI / deploy
```

Why not auto-migrate on app boot:

- Failed migration ≠ failed boot. Separate failure modes, separate rollback procedures.
- Production deploys: migrate first, verify schema, then deploy new code. Standard two-step.
- No race when multiple server instances start simultaneously.
- Roll forward / back independently of code releases.

The local workflow is `local-infra-up` → `db-migrate` → `run`, with each step run explicitly. No Flyway dep in the JVM app — the CLI owns it.

## PE shape vs domain shape

`<Entity>PEConverter` is the projection between domain and persistence. Domain types stay at the domain; PEs match the schema's column shape (which may diverge from the domain — denormalized fields, computed columns, audit metadata). Pulling smart-constructed domain types like `Email` into the PE creates conflicts the schema can't accommodate; the converter handles the flattening on the way out and the parse on the way in. See [`correct-by-construction.md`](correct-by-construction.md#domain-types-stay-at-the-domain) for the principle.

## PE ownership (the `pe-layout` principle)

PEs default to the ctx whose repo owns the table — `customer/impl/service/repo/sql/entity/CustomerPE.scala`, not `lib/common`.

Rationale: this template is built to demonstrate bounded-context separation; centralizing PEs in `lib/common` would mean the lib layer knows every context's persistence shape, which contradicts the point. The ctx-local default also fails loudly when boundaries are wrong (cross-ctx PE imports become visible in the build graph) instead of failing silently (everything in one shared bag).

When two contexts genuinely read the same table:

1. **Cross-ctx import** — the reading ctx imports the owning ctx's PE. Cheapest, fine for occasional sharing.
2. **Local projection** — the reading ctx defines its own PE for the projection it cares about. Useful when the two contexts read different column subsets or want different field types.
3. **Promote to lib** — only for entities owned by no single ctx (audit log, outbox). Goes in `lib/common/impl/repo/sql/entity/`.

PEs never leave `impl/`. Repo trait signatures use domain types; PE→domain conversion happens in the repo method body via `<Entity>PEConverter`. This is enforced by import direction (the `import-direction` principle) plus the fact that domain-layer files have no reason to import anything from `impl/`.

## DbSchema mixin

`<Name>DbSchema` is a trait holding the Quill `querySchema` declarations for that ctx's tables. The repo impl mixes it in:

```scala
trait CustomerDbSchema {
  val ctx: SqlContext
  import ctx._
  protected val customerTable = quote(querySchema[CustomerPE]("customer"))
}

final class CustomerRepoMySQLImpl(val ctx: SqlContext, transactor: Transactor)
    extends CustomerRepo
    with CustomerDbSchema { ... }
```

Splitting the schema out of the impl keeps the impl focused on query logic and makes the table-name source obvious. With `SnakeCase` naming on the context, the explicit `("customer")` is only needed when the on-disk name diverges from the type name minus the `PE` suffix.

## The `Transactor` (and the `tx-default` principle)

```scala
trait Transactor {
  def withTransaction[A](io: AppIO[A]): AppIO[A]
}
```

**Default rule:** every repo method opens a transaction.

```scala
override def find(id: CustomerId): AppIO[Option[Customer]] =
  transactor.withTransaction {
    val q = quote(customerTable.filter(_.id == lift(id)))
    ctx.runQuerySingleResult(run(q)).map(_.map(CustomerPEConverter.toCustomer))
  }
```

**App services may wrap.** When an app-service method needs two repo calls atomic, it wraps them:

```scala
def transferCredits(from: CustomerId, to: CustomerId, amount: Int): AppIO[Unit] =
  transactor.withTransaction {
    repo.debit(from, amount) *> repo.credit(to, amount)
  }
```

The inner `withTransaction` calls in the repo methods become no-ops (Quill's `transaction` is reentrant on a fiber-local connection — the inner block sees the outer's connection and doesn't open a new SQL transaction).

**Why both layers, not just one:**

- Repos always wrap → consistent isolation guarantee for any repo call, not just orchestrated ones. RLS-style `SET LOCAL` extensions plug in here without changing every call site.
- App services may wrap → atomicity across multiple repos is expressible at the layer that owns the orchestration. No need to push multi-step business logic down into the repo.

## Error translation

The chain is:

1. Quill surfaces JDBC `SQLException` on its native `Throwable` channel.
2. `SqlContext.runQuery` wraps it as `DbError` via `mapError` — per-query JDBC failures enter the `AppFailure` channel here.
3. Repo impls may `catchSome` on domain-meaningful constraint violations (unique-key → `AlreadyExistsError`, FK → custom error) before the result reaches the service. Optional, where it adds value.
4. `Transactor.withTransaction` runs Quill's `transaction`, which widens back to `Throwable`. A total `mapError` narrows: `AppFailure` passes through, `SQLException` becomes `DbError`, anything else becomes `InternalServerError`.
5. Route renders `AppFailure` as `ErrorTO`.

## Wiring

```
ConfigBootstrap.layer  → DataSourceConfig.layer
                          → DataSourceLayer.layer
                            → SqlContext.layer
                              → TransactorQuillImpl.layer
                                → <Ctx>RepoMySQLImpl.layer → ...
```

`DataSourceLayer` is a thin Hikari constructor over a typed `DataSourceConfig` — see [`config.md`](config.md) for the typed-config pattern. Migrations are applied out-of-process; the server assumes the schema is in place when it starts.

## Local dev

```sh
just local-infra-up                  # MySQL + Jaeger containers, blocks until healthy
just db-migrate                # apply Flyway migrations
just run                       # server in foreground
just start-fresh-local-server  # local-infra-reset + db-migrate + seed + run, all in one
just local-infra-reset               # wipe + restart (drops the MySQL volume; re-run db-migrate after)
```

Connection config lives in `app/server/src/main/resources/application-local.conf` (gitignored, copied from `application-local.conf.example` on `just initial-setup`). Default targets `localhost:3306/localDatabase` with `localUser`/`localPassword`, matching `docker-compose.yml`. See [`config.md`](config.md) for the broader config pattern.

## RLS — out of scope, on purpose

The `Transactor` signature is plain `AppIO[A] => AppIO[A]`. If RLS becomes a requirement later, the trait grows an auth-context parameter (or reads one from `ZIO.service`), and the impl issues `SET LOCAL app.client_id=...` at the start of each tx. The migration touches every repo call, but only once.
