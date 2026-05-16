# Prose

The discipline this codebase asks of docs, scaladoc, code comments, and review reports. The intent is the same as for code: a future reader, without the writer's in-head context, reads the artifact and acts on it.

## The standard

Good prose in this codebase:

1. **Stands on its own.** A reader who's never seen the artifact can read it cold and understand what it's about, without descending into other files for premise. This is [local reasoning](local-reasoning.md) applied to prose.

2. **Earns every sentence.** Each sentence informs the reader of something they didn't already get from the surrounding sentences. Sentences that exist for emphasis, reassurance, or rhythm get cut.

3. **Trusts the reader.** The audience is an experienced practitioner — state the claim.

## Antipatterns

### Against "stands on its own"

- **Temporal-locality leaks.** "now", "no longer", "previously", "we used to", "the refactor", "recently" — language that only makes sense if you know what was there before.
- **Diff-describing prose.** "we removed X", "this addresses the problem of Y", "shorter than before" — describes a change to the artifact, not the artifact.
- **References that route the reader elsewhere without naming what's there.** "see `FileB`" without saying what's at FileB; "as discussed in the meeting"; "mirrors X" without saying which property mirrors.
- **Backward-compat claims without naming the counterparty.** "kept for backward compat" with no statement of what it's compatible with.
- **Content the reader can't act on without info not in the artifact.** Instructions that assume a tool, command, or convention the artifact doesn't introduce.

### Against "earns every sentence"

- **Triple-explaining one idea.** The same point made three different ways, hoping one lands.
- **"Note:" / "Important:" tags carrying no info beyond emphasis.**
- **Over-citation.** Principle slugs repeated when the surrounding text already invokes the rule.
- **Self-references that need chasing.** "see § two paragraphs up" when inlining would be clearer.

### Against "trusts the reader"

- **Hedging the prose doesn't need.** "probably", "generally", "mostly", "tends to", "in most cases" when the real claim is unhedged.
- **Defensive framing.** "load-bearing", "does real work", "subtle but important".
- **Pre-empting objections nobody raised.** "you might think X but actually Y" when X wasn't on the table.
- **Justifications addressed to an imagined skeptic.**

## For rule docs

A rule or standard doc states the target first — what good looks like — and bounds it with antipatterns. A negative-only list of failures, without a positive frame, is harder to apply: the reader needs a beacon, not just terrain to avoid.
