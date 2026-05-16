---
description: Single-agent fresh-eyes read of a named file (or files). Catches the "I wrote this from inside my own head" defects — temporal-locality leaks, references that route elsewhere, defensive framing. One agent, one file-or-files target. Use after large doc / skill / scaladoc writes where the artifact will be read later without the session context.
---

# fresh-read

One sub-agent reads the named file(s) with no context about why they exist or how they got written.

## When to use

- You just wrote a new doc, skill, or instruction file.
- You heavily revised an existing doc.
- A long scaladoc block on a central type needs a sanity check.
- The artifact isn't visible in `git diff`.

## When to skip

- Small edits.
- Code-shaped changes — `/review-changes` is better suited.
- Internal scratch.

## Arguments

A file path or paths. If omitted, ask the user what to read.

## Dispatch

One `Agent` call (`subagent_type: general-purpose`) with the file paths and the mandate below.

### Mandate (passed to the sub-agent verbatim)

You're reading the named files cold. Treat yourself as a future reader who has never seen these files before, doesn't know why they exist, and didn't sit in the conversation that produced them.

Read only the named files. Don't grep, don't open siblings, don't follow internal references to other files. If the artifact under review says "you must read X first" and won't make sense otherwise, you may open X — but flag in your report that the artifact's standalone shape depends on X.

Read `docs/knowledge-base/patterns/prose.md` for the standard and antipatterns. Flag prose that violates the standard, with the fresh-eyes lens: defects that depend on the writer's in-head context — temporal-locality leaks, references routing elsewhere, defensive framing, content the reader can't act on without info not in the artifact — surface most readily from a cold read.

Bias toward cuts; flag gaps only when the artifact's stated purpose is unattainable without the missing content.

For each finding: `file:line — quoted phrase — what's wrong — what to cut / move / rephrase`.

End with a one-line verdict: `Reads clean` / `Minor cleanup` / `Substantial rework`.

## Presenting back to the user

- `Reads clean` — say so in one sentence.
- `Minor cleanup` / `Substantial rework` — surface the top 3 findings inline; point to the full agent report.

Don't auto-apply fixes. Surface, let the user decide.
