# Dev tools

`appDev` is a deployment unit for local-only one-off code: data seeding, scratchpad experiments, ad-hoc debugging actions. It lives at `modules/app/dev/` and depends on `libCommon`, `libDb`, plus the ctx modules.

## Scope

Belongs in `appDev`:

- **One-off scripts** against a real local datasource — seeding, schema introspection, data backfills.
- **Scratchpad experiments** — "let me try this thing once and see what happens." `Experiment.scala` is the empty entrypoint, with a `timed` helper.
- **Debugging actions** — small ad-hoc effects that poke the system using production layers.

Does not belong in `appDev`:

- **Production code paths.** Reachable-in-production code lives in a context module or `appServer`.
- **Tests.** Specs go in `*/src/test/scala/`.
- **Generic infrastructure.** Cross-app reusable code lives in `lib/common`.

## Local-only by build

`publish / skip := true` in the `build.sbt` entry excludes `appDev` from any deployable artifact — no registry, no docker image. The single config file is `application-local.conf` (no `dev` / `prod` variants); a desire for one is a sign the script doesn't belong here.

## Each entrypoint composes its own layers

No shared `DevEnv.layer`. Each `ZIOAppDefault` builds what it needs in its own `provide(...)` — an action that only touches the database provides `ConfigBootstrap`, `DataSourceConfig`, `DataSourceLayer`, `SqlContext`, plus `TransactorQuillImpl` if it transacts. The action's effect declares its env at the type level (`AppRIO[SqlContext & Transactor, Unit]`); the runner's `provide(...)` matches it.

Conf loads from `application-local.conf` in `appDev`'s own resources. `appDev` carries the `database.data-source` block, duplicated across apps that share the local MySQL.

## Action shape

```scala
object SeedExample extends ZIOAppDefault {
  val seed: AppRIO[SqlContext & Transactor, Unit] = ...

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    seed.provide(
      ConfigBootstrap.layer,
      DataSourceConfig.layer,
      DataSourceLayer.layer,
      SqlContext.layer,
      TransactorQuillImpl.layer
    )
}
```

The val/runner split lets other entrypoints compose the action's effect without the runner's layer stack. Invocation: `sbt "appDev/runMain com.example.app.dev.actions.SeedExample"`, fronted by a per-action `just` recipe.

## Quill `run` collision

`ZIOAppDefault` and Quill's context both expose `def run`. Inside an action body, `import ctx._` brings Quill's `run` into scope and the two collide. Exclude and qualify:

```scala
import ctx.{run => _, _}
ctx.runQuery(ctx.run(q)).unit
```

Repos sidestep this — they're regular classes with no `def run` from a parent.

## Relationship to migrations

- **Flyway migration** (`lib/db/.../db/migration/`): structural (DDL). Versioned, runs in every env.
- **Dev seed action**: data-only, runs only locally. Idempotency is the action's responsibility.

Fixture rows in a Flyway migration would either ship to production or require env-conditional migrations.
