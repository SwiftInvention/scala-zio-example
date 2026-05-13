---
description: Multi-dimensional codebase-wide sweep. Per-module passes (3 sub-agents in parallel — prose-coherence, code-coherence, element-rules) plus a final cross-cutting sweep. Findings persist to `local-data/codebase-review.md` for action across sessions.
---

# review-codebase

Sweeps the codebase for structural and per-element issues, files findings to a persistent gitignored notes file, and stops there. Acting on findings happens in subsequent sessions — picking a chunk, fixing, marking done.

Two things distinguish this from an in-conversation review:

- **Holistic, not per-element.** Most of the sub-agents ask "does this sentence fit in this paragraph?", "does this paragraph fit in this section?", "does this file fit in this module?". One agent does per-instance style/principle checks separately.
- **Findings persist.** Output goes to `local-data/codebase-review.md` (gitignored). Stable IDs so future sessions can refer to specific findings, mark them done, and not re-discover the same issues.

## Arguments

- No argument → full sweep (every module + cross-cutting).
- A module name (`libCommon`, `ctxCustomer`, `ctxNotification`, `ctxCustomerApi`, `ctxNotificationApi`, `appServer`, `appDev`, `it`) → just that module.
- `cross-cutting` → just the final sweep (top-level docs + cross-module structural questions).
- `--resume` → check `local-data/codebase-review.md`; skip modules already present, sweep the rest.

## Modules in scope

| Module             | Folder                         |
| ------------------ | ------------------------------ |
| `libCommon`        | `modules/lib/common`           |
| `ctxCustomerApi`   | `modules/ctx/customer-api`     |
| `ctxCustomer`      | `modules/ctx/customer`         |
| `ctxNotificationApi` | `modules/ctx/notification-api` |
| `ctxNotification`  | `modules/ctx/notification`     |
| `appServer`        | `modules/app/server`           |
| `appDev`           | `modules/app/dev`              |
| `it`               | `modules/app/it`               |

Markdown docs (`docs/knowledge-base/*.md`) are not reviewed per-module — they go to the cross-cutting sweep, so they're reviewed once rather than eight times.

## Common frame for the coherence agents

Used by Agents A, B, X, Y. Skip for Agent C (per-instance style/principle checks, different shape).

The codebase's `local-reasoning.md` pattern doc says local-reasoning "applies at every scale — expression, function, class, module, context, system." A coherence sweep walks the appropriate ladder for the medium it's reviewing and asks three structural questions at each level. Use judgment — don't run a mechanical checklist.

**The three questions at every level:**

- **Containment** — does each element belong in its scope, or does it belong somewhere else?
- **Cohesion** — should two siblings be merged? Should one element be split into two?
- **Completeness** — is something missing that the scope's stated purpose implies?

**Cut > expand.** All else equal, findings that remove or merge carry a lower bar than findings that add or split. A "this should be split" finding has to name what's actively incoherent about leaving it together; a "this should be cut / merged" finding only has to name what's not earning its place. Completeness questions ("is something missing?") need stronger justification than containment / cohesion ones — only raise them when the absence creates a real gap, not when the scope could *conceivably* hold more.

**Reporting** — choose the form that fits:

- Element-level (single sentence, single line, single definition): `file:line — quoted excerpt — what's wrong — what to cut/move/rephrase`.
- Scope-level (issue spans many lines or files): `scope: <named scope> — summary — what's incoherent — suggested split/merge/move/rewrite`. No fake line numbers.

Two different decisions to keep separate:

- **What makes a quality finding** — propose a change that would meaningfully alter the shape of the artifact. Don't report "this could maybe be tightened" without naming the change. Cut > expand.
- **Whether to file at all** — when you're unsure whether something *is* a defect, file it. A noted concern is cheap to dismiss; a missed one compounds. The cut-bias is about the shape of suggestions, not about the threshold for flagging.

Pre-existing violations marked `FIXME` / `WONTFIX` are out of scope. End each report with a short verdict (`Solid` / `Minor cleanup needed` / `Substantial cleanup needed`) and a one-line characterization.

### Sentence-level prose checklist

Used by every prose-reviewing agent (A for scaladoc, X for top-level docs, Y for patterns docs). At the lowest level of every prose ladder, look for:

- **Hedging** the prose doesn't need: "probably", "generally", "mostly" when the real claim is unhedged.
- **Defensive framing**: "load-bearing", "does real work", "subtle but important", reaching for emphasis where the artifact stands without it.
- **Pre-empting objections nobody raised**: "you might think X but actually Y" when X wasn't on the table.
- **Triple-explaining one idea**: same point made three different ways.
- **Over-citation**: principle slugs repeated when the surrounding text already invokes them.
- **"Note:" / "Important:" tags** carrying no info beyond emphasis.
- **Justifications addressed to an imagined skeptic** — paragraphs arguing with someone who isn't there.
- **Transitional / version-aware framing**: "now", "no longer", "previously", "we used to", "the refactor", "recently".
- **Diff-describing prose**: "we removed X", "shorter than before".
- **References that route the reader elsewhere without naming what's there**: "see `FileB`" without saying what to look for.
- **Backward-compat scaffolding without a stated counterparty**: "kept for backward compat" without saying what's incompatible.

Real reasoning that informs the reader of a non-obvious mechanism stays. Present-tense contrasts against framework defaults ("the channel is `AppFailure`, not `Throwable`") are reader-orienting, not version-aware.

### Markdown prose ladder

The full ladder for markdown docs (used by Agents X and Y). Scaladoc has its own shorter ladder defined inside Agent A.

- **Sentence in paragraph** — apply the sentence-level checklist above.
- **Paragraph in section** — does this paragraph live under the right header? Are two paragraphs covering the same ground?
- **Section in document** — does the section earn its place? Does its header match its contents?
- **Document role** — what is this doc about in one sentence? Does the actual content match? Is it duplicating another doc? Are concerns mixed?

## Per-module pass — three agents in parallel

For one module's Scala files only. Dispatch all three in one message via the `Agent` tool with `subagent_type: general-purpose`.

### Agent A — Prose coherence (scaladoc only)

Scope: Scala doc comments (`/** ... */`) inside one module's source files. Markdown docs and code are out of scope for this agent.

Read `docs/knowledge-base/patterns/local-reasoning.md` — the "Local reasoning applies to docs, too" section is the standard. Apply the common frame (containment / cohesion / completeness) at the scaladoc-specific ladder.

**Scaladoc ladder:**

- **Sentence in scaladoc block** — apply the sentence-level prose checklist (above).
- **Scaladoc block vs its definition** — does the prose describe what's actually there? Does it earn its length? Is it a stale snapshot from an earlier shape of the code? Are essential mechanisms missing from the doc?

### Agent B — Code coherence (Scala files)

Scope: structural shape of one module's Scala source files. Per-instance style/principle violations are out of scope here — they're caught by Agent C.

Read first:
- `docs/knowledge-base/patterns/local-reasoning.md` — particularly "At every level of abstraction" and the antipatterns list.
- `docs/knowledge-base/patterns/module-layout.md` and the module's relevant patterns (e.g. `bounded-context.md` for ctxes, `persistence.md` for repo-shaped code).

Apply the common frame at the code ladder:

**Levels:**

- **Method on type** — does this method belong on this trait/class/object, or is it grafted on? Are there methods that should be moved? Two methods that should be one? An obvious method missing that the type's purpose implies?
- **Definition in file** — is this file doing one thing, or is it a junk drawer? Are there definitions here that belong in a different file? Are there two files that should be one (or one that should be two)?
- **File in module** — does the file's role match the module's stated role? Should it live in a different module?


### Agent C — Element rules (per-instance principle/style violations)

Scope: per-instance violations of the codebase's documented principles and style rules across one module's Scala files. Structural shape is out of scope (Agent B handles that); scaladoc prose is out of scope (Agent A handles that).

Read first:
- `docs/knowledge-base/architecture-principles.md` — the principle list.
- `docs/knowledge-base/styleguide.md` — the style rules.
- `docs/knowledge-base/patterns/local-reasoning.md` — for the code-level antipattern list (vestigial generality, transitional names, hidden state, action-at-a-distance).
- `docs/knowledge-base/patterns/correct-by-construction.md` — for type-discipline checks.
- Read the relevant `patterns/<...>.md` for any specific principle you need to disambiguate.

**Principles by slug**: `local-reasoning`, `correct-by-construction`, `module-layout`, `bounded-context`, `import-direction`, `build-deps`, `ctx-api`, `to-converters`, `newtypes`, `smart-constructors`, `errors`, `config-shape`, `pe-layout`, `pe-converters`, `tx-default`, `logging`. (The slug list lives in `architecture-principles.md`; if that doc adds an entry, add it here too.)

**Style rules by slug**: `impl-suffix`, `no-package-files`, `final-by-default`, `no-default-args`, `named-args`, `no-null`, `no-var`.

**Code-level local-reasoning antipatterns**: vestigial generality (`Map[K, V]` always one key, `Option[X]` always `Some`, sealed trait with one case, abstract method with one impl); names that capture transition (`newFoo`, `RefactoredFoo`, `FooV2`, flags whose `false` branch is dead); shared mutable state; ambient context not in signatures; action-at-a-distance.

A violation is real when a rule applies and the resulting code doesn't match. Pre-existing violations marked `FIXME` / `WONTFIX` are out of scope.

**Reporting** — per-instance:

- `file:line — quoted code — `principle-slug` or `style-rule-slug` — why it violates`.

When a single pattern hits many sites in this module, group them so the orchestrator can roll up.

End with a one-line verdict. Skip the report if the module is clean against these rules.

## Final cross-cutting sweep — three agents in parallel

Runs after all module passes (or by itself if invoked with `cross-cutting`). Scope: the knowledge-base docs plus the cross-module structural questions.

### Agent X — Top-level doc coherence

Scope: `docs/knowledge-base/overview.md`, `architecture-principles.md`, `styleguide.md`, `commands.md`, `domain.md`.

Apply the common frame (containment / cohesion / completeness) at the markdown prose ladder defined above.

### Agent Y — Patterns-doc coherence

Scope: `docs/knowledge-base/patterns/*.md`.

Apply the common frame at the markdown prose ladder defined above. One extra question at the document-role level: **does each pattern doc earn its place as a separate file, or do two of them cover the same ground?**

### Agent Z — Cross-module structure

Scope: the module graph in `build.sbt`, the file layouts under `modules/`, the architectural claims in `module-layout.md`, `build-deps.md`, `bounded-context.md`, `ctx-api.md`.

Apply containment / cohesion / completeness at the highest code ladder:

- **Module role** — what is each module about, in one sentence? Does its actual content match the stated role? Are there files inside that belong in a different module?
- **Module-in-system** — is each module cleanly distinct from its siblings? Are concerns mixed across modules? Are the deps in `build.sbt` honest about the architecture (does anything depend on a module it shouldn't, or fail to depend on one it does use)?

**Reporting** — scope-named:

- `scope: <module or pair of modules> — summary — what's incoherent — suggestion`.

## Orchestration

For each module (or for `cross-cutting`):

1. Dispatch the three agents in parallel in a single message via the `Agent` tool (`subagent_type: general-purpose`).
2. Collect the three reports as they return.
3. **Dedupe** — if two agents flag the same underlying issue (e.g. Agent A finds "scaladoc duplicates pattern-doc text" and Agent C finds "scaladoc over-cites the principle slug"), keep the better-positioned one and cross-reference rather than double-filing.
4. **Assign IDs** — next free `F###` from `local-data/codebase-review.md`. Pattern rollups get `P-<slug>`.
5. **Append** to the findings file under the right section. Update the `Last updated` line at the top.

Do not fix anything in this skill — its output is the findings file, not code changes.

## Findings file format

`local-data/codebase-review.md`:

```markdown
# Codebase Review

Last updated: YYYY-MM-DD

## Pattern rollups

- `P-named-args` — N sites — see F00x, F00y, ...
- `P-vestigial-Option` — N sites — see F0zz

## <module-name>

Swept YYYY-MM-DD.

### Coherence (prose)
- [F001] `scope: <…>` — summary — what's incoherent — suggestion

### Coherence (code)
- [F002] `scope: <…>` — summary — suggestion

### Element-level
- [F003] `file:line` — code — `principle-slug` — why
- [F004] ...

## cross-cutting

Swept YYYY-MM-DD.

### Top-level docs
- [F050] ...

### Patterns docs
- [F060] ...

### Cross-module structure
- [F070] ...
```

A finding is identified by its `F###`. To mark one done in a future session, prepend `~` (`[~F001]`). To mark `wontfix`, append `wontfix` and a one-line rationale: `[F001 wontfix] reason`. The rationale is required.

Each module/section carries its own `Swept YYYY-MM-DD` line; `--resume` reads these to decide what to skip.

### Re-sweeping

When a module or `cross-cutting` is swept again, append the new findings under the existing section with new `F###` IDs; don't replace what's there. Existing findings keep their status (open / `~` done / `wontfix`). If a new finding restates the root cause of an open finding already in the file, cross-reference it (`[F120] — duplicate root cause as F050; resolving F050 resolves both`) rather than re-filing. Update the section's `Swept` line to the latest date and tag round-N findings with `(Round N)` so chronology survives the append.

Re-sweeps aren't just "did the cleanup land?" — they also catch what the previous sweep missed. After implementing a chunk, the eyes are sharper and the agents flag things that slipped through round 1.

## Presenting back to the user

After the sweep:

1. Append everything to the findings file.
2. Print a concise summary inline: total findings, breakdown by category, top 5 pattern rollups by site count, and 2-3 of the most structurally interesting coherence findings (the "this would change the shape" ones, not the "this sentence could be tighter" ones).
3. Don't dump full agent reports — they're in the file. Mention the file path so the user can read at leisure.
4. Don't ask "what do you want to fix?" — that's a separate session's question.

When `--resume` is in effect, mention which modules were skipped and why ("`libCommon` already swept on YYYY-MM-DD; rerun with explicit module name to re-sweep").

## Iterating to diminishing returns

A codebase sweep isn't one-shot. The intended loop:

1. Run a sweep (cross-cutting first, then per-module).
2. Triage with the user; deduplicate across the batch.
3. Implement a chunk of findings (mechanical cuts first, structural last).
4. Re-run the affected scope.

After implementation, several findings often dissolve — fixing one cross-doc duplication invalidates the matching one on the other side, and pattern-rollup cuts knock out individual sites the rollup pointed at. Re-running surfaces what the cleaned shape looks like.

When a chunk creates a new doc, run `/fresh-read` on it before moving on. New writes carry the writer's in-head context as defensive framing, restatements, and references that route elsewhere — `fresh-read` catches what `review-changes` and a `diff` glance miss.

Stop when the latest sweep batch produces a list of findings that can mostly be ignored (empty, nitpicking, stuff we already dismissed previously). That's the diminishing-returns signal — say so explicitly when you observe it.
