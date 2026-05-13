# Persistence

Quill + MySQL + Flyway, with explicit transaction boundaries.

## Layout

`lib/db` is the shared persistence module: one schema for the whole deployment, plus the infrastructure that talks to it — migrations, PEs, DbSchema traits, `SqlContext`, `DataSourceLayer`, `Transactor` + impl, `NewTypeEncodings`.

```text
modules/lib/db/src/main/scala/com/example/lib/db/
├── domain/service/
│   └── Transactor.scala                  trait — transaction boundary
└── impl/
    ├── repo/sql/
    │   ├── DataSourceConfig.scala        typed DB config (PureConfig case class + layer)
    │   ├── DataSourceLayer.scala         Hikari pool from DataSourceConfig
    │   ├── SqlContext.scala              Quill JDBC context + encodings
    │   ├── NewTypeEncodings.scala        MappedEncodings for Newtype ids
    │   ├── schema/
    │   │   └── <Name>DbSchema.scala      Quill querySchema declarations per table
    │   └── entity/
    │       └── <Name>PE.scala            case class mirroring the table
    └── service/
        ├── TransactorQuillImpl.scala     Transactor impl
        └── DbProbeQuillImpl.scala        DbProbe impl — SELECT 1 against SqlContext

modules/lib/db/src/main/resources/
└── db/migration/
    └── V<date>.<seq>__<name>.sql         Flyway-versioned migrations (applied out-of-process)

modules/ctx/<name>/src/main/scala/com/example/ctx/<name>/
├── domain/service/repo/
│   └── <Name>Repo.scala                  repo trait — domain types only
└── impl/service/repo/
    ├── <Name>RepoMySQLImpl.scala         MySQL impl (per impl-suffix rule)
    └── converter/
        └── <Name>PEConverter.scala       PE ↔ domain mapping
```

The `sql/` package and `SqlContext` name are dialect-agnostic — `MySQLImpl` is the dialect-specific suffix on the repo impl, the rest is portable.

## PE ownership

The schema is one schema. Migrations apply as one batch; PEs are the in-Scala mirror of that batch. A foreign-table consumer imports the PE from `lib/db` like any other library type.

What `lib/db` ships: PEs (data shapes), DbSchema traits (Quill `querySchema` declarations), and the infrastructure to run queries. It has no awareness of any ctx's domain.

What each ctx keeps: the repo trait (signatures in domain types), the repo impl (queries against PEs + conversion), the `<Name>PEConverter`. The converter stays in the ctx because it's the only place both the PE and the domain entity are visible.

## Migrations

**Location:** `lib/db/src/main/resources/db/migration/`.

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

`<Entity>PEConverter` is the projection between domain and persistence. Domain types stay at the domain; PEs match the schema's column shape (which may diverge from the domain — denormalized fields, computed columns, audit metadata). Pulling smart-constructed domain types like `Email` into the PE creates conflicts the schema can't accommodate; the converter handles the flattening on the way out and the parse on the way in.

## DbSchema mixin

`<Name>DbSchema` is a trait holding the Quill `querySchema` declarations for one table. The repo impl mixes it in:

```scala
// lib/db/.../impl/repo/sql/schema/CustomerDbSchema.scala
trait CustomerDbSchema {
  val ctx: SqlContext
  import ctx._
  protected val customerTable = quote(querySchema[CustomerPE]("customer"))
}

// ctx/customer/.../impl/service/repo/CustomerRepoMySQLImpl.scala
final class CustomerRepoMySQLImpl(val ctx: SqlContext, transactor: Transactor)
    extends CustomerRepo
    with CustomerDbSchema { ... }
```

Splitting the schema out of the impl keeps the impl focused on query logic and makes the table-name source obvious. With `SnakeCase` naming on the context, the explicit `("customer")` is only required when the on-disk name diverges from the type name minus the `PE` suffix.

A ctx querying multiple tables (its own + a foreign one for a join) mixes in multiple `DbSchema` traits.

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

Quill's `transaction` is reentrant on the fiber-local connection, so the inner `withTransaction` calls join the outer scope rather than opening a new SQL transaction.

Roles of each layer:

- Repos always wrap → consistent isolation guarantee for any repo call, not just orchestrated ones.
- App services may wrap → atomicity across multiple repos is expressible at the layer that owns the orchestration, without pushing multi-step business logic down into the repo.

## Error translation

The chain is:

1. Quill surfaces JDBC `SQLException` on its native `Throwable` channel.
2. `SqlContext.runQuery` wraps it as `DbError` via `mapError` — per-query JDBC failures enter the `AppFailure` channel here.
3. Repo impls may `catchSome` on domain-meaningful constraint violations (unique-key → `AlreadyExistsError`, FK → custom error) before the result reaches the service. Optional, where it adds value.
4. `Transactor.withTransaction` runs Quill's `transaction`, which widens back to `Throwable`. A total `mapError` narrows: `AppFailure` passes through, `SQLException` becomes `DbError`, anything else becomes `InternalServerError`.
5. Route renders `AppFailure` as `ErrorTO`.

## Readiness probe

`DbProbe` (trait in `lib/common/domain/service/`, impl `DbProbeQuillImpl` in `lib/db/impl/service/`) exposes a single-round-trip reachability check. `HealthRoutes` (in `lib/common`) consumes the trait for `/ready`. Since `lib/db` depends on `lib/common`, the trait has to live in `lib/common` — placing it in `lib/db` would close a cycle.

## Wiring

```text
ConfigBootstrap.layer  → DataSourceConfig.layer
                          → DataSourceLayer.layer
                            → SqlContext.layer
                              ├─→ TransactorQuillImpl.layer
                              │     → <Ctx>RepoMySQLImpl.layer → ...
                              └─→ DbProbeQuillImpl.layer
                                    → HealthRoutes.layer
```

`DataSourceLayer` is a thin Hikari constructor over a typed `DataSourceConfig` — see [`config.md`](config.md) for the typed-config pattern. Migrations are applied out-of-process; the server assumes the schema is in place when it starts.

## Local dev

Connection config lives in `app/server/src/main/resources/application-local.conf`. Default targets `localhost:3306/localDatabase` with `localUser`/`localPassword`, matching `docker-compose.yml`. See [`commands.md`](../commands.md) for the local infra / migrate / run recipes, and [`config.md`](config.md) for the broader config pattern.
