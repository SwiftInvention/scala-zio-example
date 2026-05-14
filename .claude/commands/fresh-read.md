---
description: Single-agent fresh-eyes read of a named file (or files). Catches the "I wrote this from inside my own head" defects — temporal-locality leaks, references that route elsewhere, defensive framing. One agent, one file-or-files target. Use after large doc / skill / scaladoc writes where the artifact will be read later without the session context.
---

# fresh-read

One sub-agent reads the named file(s) with no context about why they exist or how they got written. The lens is "stranger walking in" — does the artifact stand on its own?

## When to use

- You just wrote a new doc, skill, or instruction file.
- You heavily revised an existing doc.
- A long scaladoc block on a central type needs a sanity check.
- The artifact isn't visible in `git diff` (already committed; not tracked yet; or you only want this one file reviewed in isolation).

## When to skip

- Small edits — the cost-benefit isn't there.
- Code-shaped changes — `/review-changes` (three-agent diff review) is better suited.
- Internal scratch.

## Arguments

A file path or paths. If omitted, ask the user what to read.

## Dispatch

One `Agent` call (`subagent_type: general-purpose`). Pass the file paths and the mandate below in the prompt.

### Mandate (passed to the sub-agent verbatim)

You're reading the named files cold. Treat yourself as a future reader who has never seen these files before, doesn't know why they exist, and didn't sit in the conversation that produced them.

Read only the named files. Don't grep, don't open siblings, don't follow internal references to other files — model a reader who only has what's in front of them. If the artifact under review says "you must read X first" and won't make sense otherwise, you may open X — but flag in your report that the artifact's standalone shape depends on X.

Look for defects that depend on the writer's in-head context:

- **Temporal-locality leaks**: "now", "no longer", "previously", "we used to", "the refactor", "recently" — language that only makes sense if you know what was there before.
- **References that route elsewhere without naming what's there**: "see `FileB`" without saying what's in FileB; "as we discussed"; "mirrors X" without saying which property is mirrored.
- **Defensive framing**: "load-bearing", "does real work", "subtle but important" — emphasis that signals "trust me on this" instead of standing on the content.
- **Pre-empting objections nobody raised**: "you might think X but actually Y" when X wasn't on the table.
- **Justifications addressed to an imagined skeptic** — paragraphs arguing with no one.
- **Triple-explaining one idea**: the same point made three different ways.
- **"Note:" / "Important:"** tags carrying no info beyond emphasis.
- **Hedging** the prose doesn't need.
- **Self-references to other parts of the file that require chasing**: "see § two paragraphs up" when inlining would be clearer.
- **Content the reader can't act on without information not in the artifact**: instructions that assume a tool, command, or convention the artifact doesn't introduce.

Bias toward cuts; flag gaps only when the artifact's stated purpose is unattainable without the missing content.

For each finding: `file:line — quoted phrase — what's wrong — what to cut / move / rephrase`.

End with a one-line verdict: `Reads clean` / `Minor cleanup` / `Substantial rework`.

## Presenting back to the user

- `Reads clean` — say so in one sentence.
- `Minor cleanup` / `Substantial rework` — surface the top 3 findings inline; point to the full agent report.

Don't auto-apply fixes. Surface, let the user decide.
