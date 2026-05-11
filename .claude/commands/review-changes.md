---
description: Multi-dimensional fresh-eyes review of working-tree changes. Three sub-agents in parallel — docs & comments, principle adherence, domain & task-specific choices.
---

# review-changes

Three sub-agents in parallel, each looking at the working-tree diff through a different lens. The fresh-eyes property is the value — none of them have your context.

Dimensions:

- **Docs & comments** — prose quality. Local-reasoning at the prose level; "perfect is nothing left to cut away" against hedging, defensive framing, and over-justification.
- **Principle adherence** — whether the code follows the codebase's documented principles and style rules. Includes code-level local-reasoning and correct-by-construction.
- **Domain & task-specific choices** — domain-model fitness, plus design decisions outside the documented principles (deps, abstractions, idioms, edge cases).

## How to invoke

In one message, dispatch all three Agent tool calls (subagent_type: `general-purpose`) so they run in parallel. The prompts below are starting points — adapt scope to the actual diff if it helps. Triage their output before presenting (see "Presenting the results").

---

## Agent 1 — Docs & comments

You're reviewing prose only — `.md` files in `docs/` and Scala doc comments (`/** ... */`). Code-level concerns are out of scope.

Two checks:

### Check 1: Local reasoning at the prose level

Read `docs/knowledge-base/patterns/local-reasoning.md` — the "Local reasoning applies to docs, too" section is the standard.

Things to look for:

1. Transitional / version-aware framing: "now", "no longer", "previously", "we used to", "as we changed", "the refactor", "recently".
2. Diff-describing prose: "shorter than before", "we removed X", "this addresses the problem of Y" — describes a change, not the artifact.
3. References that route the reader elsewhere without saying what's there: "see `FileB`" without naming what to look for; "as documented in the meeting"; comments that gesture rather than describe.
4. Backward-compat scaffolding without a stated counterparty: "kept for backward compat" without saying what's incompatible.

A cross-reference that names what's at the destination is fine ("see `local-reasoning.md` for the foundational frame"). Present-tense contrasts against framework defaults ("the channel is `AppFailure`, not `Throwable`") are reader-orienting, not version-aware.

### Check 2: "Perfect is nothing left to cut away"

The audience is a skilled, experienced reader. Trust them. Cut anything that smooths over a confidence gap rather than informing.

Things to look for:

1. **Hedging** the prose doesn't need: "probably", "generally", "mostly", "tends to", "in most cases" when the real claim is unhedged.
2. **Defensive framing**: "load-bearing", "does real work", "subtle but important", "the triple X is required because" — reaching for emphasis where the artifact stands without it.
3. **Pre-empting objections nobody raised**: "you might think X but actually Y" when X wasn't on the table.
4. **Triple-explaining one idea**: same point made three different ways, hoping one lands.
5. **Over-citation**: "per the X principle" repeated when the surrounding text already invokes it; principle slugs in every paragraph instead of once.
6. **"Note:" / "Important:" tags** carrying no info beyond emphasis.
7. **Justifications addressed to an imagined skeptic** — paragraphs that argue with someone who isn't there.

Real reasoning that informs the reader of a non-obvious mechanism stays (e.g. "Quill's transaction is reentrant on a fiber-local connection, so nested calls reuse the outer scope" — the reader benefits from knowing this). Defensive emphasis on choices that don't need defending goes.

### Reporting

For each finding: `file:line — quoted phrase — local-reasoning OR cut-to-bone — what to cut or rephrase`. When in doubt, flag it — a noted concern is cheap to dismiss, a missed one compounds. End with a verdict (Solid / Minor cleanup needed / Substantial cleanup needed) and skip the report if you didn't find anything.

---

## Agent 2 — Principle adherence

You're reviewing code for adherence to the codebase's documented principles and style rules. Prose in `.md` docs and Scala doc comments is out of scope.

**Read first:**
- `docs/knowledge-base/architecture-principles.md` — the principle list.
- `docs/knowledge-base/styleguide.md` — the style rules.
- `docs/knowledge-base/patterns/local-reasoning.md` — for code-level local-reasoning checks (vestigial generality, action-at-a-distance, hidden state).
- `docs/knowledge-base/patterns/correct-by-construction.md` — for type-discipline checks (legal-state space, smart constructors, parse-don't-validate, domain types stay at the domain).

Read the relevant `patterns/<...>.md` for any specific principle you need to disambiguate.

**Principles by slug** (see `architecture-principles.md`): `local-reasoning`, `correct-by-construction`, `module-layout`, `bounded-context`, `import-direction`, `build-deps`, `ctx-api`, `to-converters`, `newtypes`, `smart-constructors`, `errors`, `config-shape`, `pe-layout`, `pe-converters`, `tx-default`, `logging`, `tracing`.

**Style rules by slug** (see `styleguide.md`): `impl-suffix`, `no-package-files`, `final-by-default`, `no-default-args`, `named-args`, `no-null`, `no-var`.

**Code-level local-reasoning checks** (prose-level local-reasoning is out of scope):

- Vestigial generality: a `Map[K, V]` that only ever holds one key; an `Option[X]` always `Some`; a sealed trait with one case; an abstract method with one impl.
- Names that capture transition: `newRouteHandler`, `RefactoredFoo`, `FooV2`, `useCachedX = true` flags whose `false` branch is dead.
- Shared mutable state, ambient context not declared in signatures, action-at-a-distance.

A violation is real when a rule applies and the resulting code doesn't match. Pre-existing violations marked `FIXME` / `WONTFIX` are out of scope — focus on what the change introduces or leaves unmarked.

For each finding: `file:line — quoted code — \`principle-slug\` or \`style-rule-slug\` — why it violates`. When in doubt, flag it — a noted concern is cheap to dismiss, a missed one compounds. End with a verdict; skip the report if the diff is clean.

---

## Agent 3 — Domain & task-specific choices

You're reviewing two adjacent things:

- **Domain-model fitness** — whether new types, values, and operations capture the actual domain.
- **Task-specific design choices** — decisions outside the codebase's documented principles. Where a principle exists for a kind of decision, it's checked elsewhere; where the principles are silent, you check.

**Read first:**
- `docs/knowledge-base/domain.md` — the codebase's domain.
- Any `patterns/*.md` that touches the diff (e.g. `persistence.md` if PEs changed, `errors.md` if new error types, `config.md` if new XConfigs).

**Domain-model questions:**

1. Invariants in prose instead of types? "Must be non-empty", "must be unique within X" — could a Newtype, smart constructor, or sealed enforce it?
2. Primitive obsession? `String`/`Int` where the value has structure.
3. Wrong shape for the legal-state space? `Boolean` where three states exist; `Option[X]` where two presence-meanings conflate; sealed-trait single-case that should be a value.
4. Operation granularity mismatch? A method taking a whole entity to update one field; a service trait bundling unrelated operations.
5. Wire-format leakage in TOs? Internal flags, denormalized fields, persistence-shape detail.
6. Implicit business assumptions about external shape (timezone, ordering, uniqueness, format) not asserted at the boundary.

**Task-specific design questions:**

1. New deps — transitive risk, maintenance status, alternatives the codebase already has?
2. New abstractions — right level, over-engineered, under-typed for the use?
3. New configuration shapes — does the type match the semantic distinction (e.g. `Option[X]` vs sealed ADT for binary on/off)?
4. Idiom choices — did the change reach for the right framework abstraction, or roll its own when the framework had one?
5. Edge cases the design doesn't handle (empty inputs, concurrent calls, failure modes, cleanup)?

For each finding: `file:line or area — what's coded — what's at issue — what would tighten it — confidence (strong / soft)`. When in doubt, flag it as soft — a noted concern is cheap to dismiss, a missed one compounds. The `confidence` field is the calibration mechanism; use it. End with a verdict.

---

## Presenting the results

You're triaging the agents' output before the user sees it. The failure mode to avoid: dumping all three reports verbatim with "what do you want to act on?" — that pushes synthesis onto the user. Instead, categorize and apply where you can.

A shape that's worked:

```
## Obvious yes (applied)
- <finding-id> — <one-line reason>

## Obvious no (skipped)
- <finding-id> — <one-line reason>

## Murky (your call)
<AskUserQuestion if there's a real decision; otherwise just describe>
```

- **Obvious yes** — clear issue, fix is mechanical and low-risk. Stale references, prose to cut, type narrowing the chain already supports, dead config. Apply directly.
- **Obvious no** — the agent flagged it soft, the issue is interop with an external API, out-of-scope, or genuinely defensible from current state. Skip.
- **Murky** — judgment calls about scope, naming, design trade-offs. If there's a real decision to surface, ask via `AskUserQuestion` (group related findings into one question). If not, describe and move on — don't manufacture a question because the structure expects one.

List every finding with its category and one-line reason — the triage is visible, not a black box. The full agent reports remain in the conversation; refer back if a finding needs more context.

When repeated invocations within a session start producing only marginal findings, say so — "diminishing returns" is a useful signal back to the user that the diff is settled.
