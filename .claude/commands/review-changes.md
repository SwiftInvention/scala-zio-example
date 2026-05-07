---
description: Fresh-eyes sweep of working-tree changes against local-reasoning.md, hunting for self-blind violations the author can't see.
---

# review-changes

Dispatch a sub-agent to review the current working-tree diff (`git diff` + untracked files) for local-reasoning violations specifically — not a general code review.

**Why this exists:** when you write code or docs, you do it while remembering the previous state of the file. That history is invisible to a fresh reader landing on the file cold, but it leaks into the writing as transitional or version-aware framing that reads natural to the author and disorienting to anyone else. This sweep is the counterweight to that bias — a fresh pair of eyes that doesn't have the history.

Use the **general-purpose** sub-agent. Hand it this prompt verbatim (substituting the actual diff scope):

---

You're reviewing the current working-tree changes in this repo for local-reasoning violations specifically. This is a focused sweep, not a general code review.

**Read first:** `docs/knowledge-base/patterns/local-reasoning.md` — that's the standard. Pay attention to the "Antipatterns" section and "Local reasoning applies to docs, too."

**The specific failure mode to hunt:** transitional / version-aware framing that only parses if the reader remembers a previous state. The author wrote it while remembering the old state, so the phrasing reads natural to them but routes a fresh reader to context they don't have.

Categories to flag:

1. **Transitional language in comments/docs:** "now", "no longer", "previously", "we used to", "as we changed", "the refactor", "recently", "this used to be X but is now Y".
2. **Diff-describing prose:** "shorter than before", "we removed X", "this addresses the problem of Y" — describes a change, not the artifact.
3. **Vestigial generality in code:** a `Map[K, V]` that only ever holds one key because it used to hold many; an `Option[X]` that's always `Some` because the `None` path was deleted; a sealed trait with one case because the others were collapsed; an abstract method overridden by exactly one impl.
4. **Names that capture transition:** `newRouteHandler` (when there's no old one), `RefactoredFoo`, `FooV2`, `useCachedX = true` flags whose `false` branch is dead.
5. **References that route the reader elsewhere without saying what's there:** "see `FileB`" without naming what to look for; "as documented in the meeting"; "per the article"; comments that gesture to other files instead of describing the local thing in its own terms.
6. **Backward-compat / migration scaffolding without a stated counterparty:** "kept for backward compat" without saying what's incompatible; "deprecated, use X instead" with no removal trigger.

**Adjacent but NOT in scope (don't flag):**

- Cross-references that *describe what's at the destination* (e.g. "see `local-reasoning.md` for the foundational frame" — names what's there).
- Present-tense statements about current state that happen to phrase a contrast against a *natural reader expectation* (e.g. "the channel is `AppFailure`, not `Throwable`" contrasts against ZIO's default — that's reader-orienting, not version-aware).
- Code-level diffs themselves — only the resulting comment/doc/code shape, not what changed in git.
- General code-quality issues (naming, structure, performance). Stay in scope.

**Scope:** Inspect every file modified in `git diff --name-only` (plus untracked tracked-worthy files via `git status`). For code files, look at the *resulting* code and comments — not the diff hunks.

For reporting, follow these rules:

- For each finding: `file:line — quoted phrase or short excerpt — why it's a local-reasoning violation`
- Be selective. Defend each finding as you would on a code review. False positives erode signal.
- Cap at the top 7 highest-confidence findings if there are more.
- If you find none, say so plainly. Do not manufacture issues to look thorough.
- End with a one-line verdict: "Solid", "Minor cleanup needed", or "Substantial cleanup needed".
- Do not fix anything — just report. ~250 words max.

---

After the agent reports, **show the findings to the user verbatim** before fixing anything — they may want to discuss which ones to address vs. let stand. Some "soft" findings have legitimate present-state defenses; the user is the deciding voice on those.
