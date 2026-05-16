---
description: Multi-dimensional sweep of the codebase. Three sub-agents in parallel — prose coherence, code coherence, element rules. Returns a triaged list of findings; doesn't persist them.
---

# review-codebase

Acting on findings happens in conversation after the sweep — the skill returns a triaged list, doesn't fix anything.

## Three scopes

- **Global** — every module + cross-cutting. Heavy. Use sparingly.
- **Per-module** — one module's Scala files (code + scaladoc). Light. Use iteratively as you clean up.
- **Cross-cutting** — top-level docs + patterns docs + module graph + cross-module structure. Light.

## Arguments

- No argument → global sweep.
- An sbt module id (discovered from `build.sbt`) → just that module.
- `cross-cutting` → just the cross-cutting scope.

Markdown docs (`docs/knowledge-base/*.md`) aren't reviewed per-module — they go to `cross-cutting`.

## The common frame

Used by Agents A and B. Agent C runs per-instance rule checks and skips this section.

A coherence sweep walks the ladder for its medium and asks three questions at each level.

**Three questions at every level:**

- **Containment** — does each element belong in its scope, or somewhere else?
- **Cohesion** — should two siblings merge? Should one element split into two?
- **Completeness** — is something missing the scope's stated purpose implies?

**Cut > expand.** Findings that remove or merge carry a lower bar than findings that add or split. Completeness findings need stronger justification still — only raise when absence creates a real gap, not when the scope could *conceivably* hold more.

**Threshold for filing:** when unsure whether something *is* a defect, file it. The cut-bias shapes *suggestions*, not the *threshold* for flagging.

## Markdown prose ladder

Used by Agent A when reviewing markdown docs:

- Sentence in paragraph — apply the sentence-level standard from `prose.md`.
- Paragraph in section — right header? Two paragraphs covering the same ground?
- Section in document — earns its place? Header matches contents?
- Document role — what's this doc about in one sentence? Does the content match? Duplicating another doc? Concerns mixed?

## The three agents

Dispatch in parallel via the `Agent` tool, `subagent_type: general-purpose`. One message, three tool calls.

### Agent A — Prose coherence

**Per-module scope:** scaladoc inside the module's Scala files. Markdown docs and code are out of scope.

**Cross-cutting scope:** all markdown under `docs/knowledge-base/` (top-level + patterns/). Scaladoc is out of scope. One extra question at the document-role level for patterns docs: does each pattern doc earn its place as a separate file, or do two cover the same ground?

Read `docs/knowledge-base/patterns/prose.md` for the sentence-level standard and antipatterns. Read `docs/knowledge-base/patterns/local-reasoning.md` for the "Local reasoning applies to docs, too" framing.

**Scaladoc ladder:**

- Sentence in scaladoc block — apply the sentence-level standard from `prose.md`.
- Scaladoc block vs its definition — does the prose describe what's actually there? Does it earn its length? Stale snapshot from an earlier shape? Essential mechanisms missing?

For markdown, use the markdown prose ladder above.

### Agent B — Code coherence

**Per-module scope:** structural shape of the module's Scala files. Per-instance style/principle violations are out of scope (Agent C catches those).

**Cross-cutting scope:** the module graph in `build.sbt`, file layouts under `modules/`, the architectural claims in `module-layout.md` / `build-deps.md` / `bounded-context.md` / `cross-context-call.md`.

Read first:

- `docs/knowledge-base/patterns/local-reasoning.md` — particularly "At every level of abstraction" + the antipatterns list.
- `docs/knowledge-base/patterns/module-layout.md` and the module's relevant patterns (e.g. `bounded-context.md` for ctxes, `persistence.md` for repo-shaped code).

Apply the common frame at the code ladder:

- **Method on type** — does this method belong on this trait/class/object, or is it grafted on? Methods that should move? Two that should be one? An obvious one missing the type's purpose implies?
- **Definition in file** — file doing one thing or a junk drawer? Definitions that belong elsewhere? Two files that should be one (or one that should be two)?
- **File in module** — does the file's role match the module's stated role? Should it live in a different module?

At cross-cutting scope, the ladder shifts to module-level questions: does each module's actual content match its stated role? Are concerns mixed across modules? Are `build.sbt` deps honest about the architecture (depends on something it shouldn't, or fails to depend on something it does use)?

### Agent C — Element rules

Scope: per-instance violations of documented principles and style rules across the module's Scala files. Structural shape is Agent B's; scaladoc is Agent A's. Usually skipped at cross-cutting scope — element rules are per-module work.

Read first:

- `docs/knowledge-base/architecture-principles.md` — the principle list.
- `docs/knowledge-base/styleguide.md` — the style rules.
- `docs/knowledge-base/patterns/local-reasoning.md` — for the code-level antipattern list.
- `docs/knowledge-base/patterns/correct-by-construction.md` — for type-discipline checks.
- The relevant `patterns/<...>.md` for any specific principle you need to disambiguate.

**Principles by slug**: `local-reasoning`, `correct-by-construction`, `module-layout`, `bounded-context`, `import-direction`, `build-deps`, `ctx-api`, `to-converters`, `newtypes`, `smart-constructors`, `errors`, `config-shape`, `db-lib`, `pe-converters`, `tx-default`, `logging`.

**Style rules by slug**: `impl-suffix`, `no-package-files`, `final-by-default`, `no-default-args`, `named-args`, `no-null`, `no-var`.

**Code-level local-reasoning antipatterns**: vestigial generality (`Map[K, V]` always one key, `Option[X]` always `Some`, sealed trait with one case, abstract method with one impl, flags whose `false` branch is dead); names that capture transition (`newFoo`, `RefactoredFoo`, `FooV2`); shared mutable state; ambient context not in signatures; action-at-a-distance.

Pre-existing violations marked `FIXME` / `WONTFIX` are out of scope.

When a single pattern hits many sites in this module, group them so the orchestrator can roll up.

## Report format

Each agent reports findings as one of:

- **Element-level**: `file:line — quoted excerpt — what's wrong — what to cut/move/rephrase`
- **Scope-level**: `scope: <named scope> — summary — what's incoherent — suggestion`

End with a one-line verdict (`Solid` / `Minor cleanup needed` / `Substantial cleanup needed`) and a one-line characterization. Skip the report if the scope is clean against the agent's rules.

## Orchestration

For each scope (one for a targeted run; one per module plus cross-cutting for global):

1. Dispatch the three agents in parallel.
2. Collect the reports.
3. **Dedupe** — if two agents flag the same underlying issue, keep the better-positioned one and cross-reference rather than double-filing.
4. **Present to the user** — a triaged list. Lead with the structurally interesting findings (those that would change shape, not just trim a sentence). Group by pattern when several share a root cause. Discuss what's there; let the user steer. Don't dump full agent reports; don't fix anything in this skill.

## Iterating to diminishing returns

Each per-module and the cross-cutting scope gets its own loop — don't chain them. (Global is a one-shot discovery sweep; the loops happen inside the granular scopes afterwards.)

1. Run the sweep on one scope.
2. Triage with the user; deduplicate.
3. Implement a chunk of findings (mechanical cuts first, structural last).
4. Re-run the same scope.

After implementation, findings often dissolve — fixing one cross-doc duplication invalidates the matching one on the other side. Re-running surfaces the cleaned shape and catches what slipped through round 1.

When a chunk creates a new doc, run `/fresh-read` on it before declaring done.

Stop when the latest sweep produces a list that can mostly be ignored (empty, nitpicking, stuff already dismissed) — say so explicitly when you observe it.

## Persistence

If you want findings to survive past the session, write them somewhere — pick whatever format fits.
