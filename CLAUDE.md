# CLAUDE.md

Here's some general info on this project:

@docs/knowledge-base/overview.md
@docs/knowledge-base/domain.md
@docs/knowledge-base/patterns/local-reasoning.md
@docs/knowledge-base/architecture-principles.md
@docs/knowledge-base/styleguide.md
@docs/knowledge-base/commands.md

There are also specific docs in `docs/knowledge-base/patterns/` that are not in context by default - consider reading some of those when you're working on a relevant area.
We always have `patterns/local-reasoning.md` in the context because it's foundational philosophy we want to keep in mind at all times.

When you write code, docs or comments, there's a tendency to write with the assumption that the reader is familiar with what you're currently familiar with. This comes out as stuff like transitional framing ("now", "no longer", "we used to"), references to a specific case when describing a generic mechanism, links to info not available to a fresh reader ("mirrors X"). To compensate for this, we have a couple of skills:

- `/fresh-read` - Single-agent fresh-eyes read of a named file (or files). Useful for any write or edit that touches a non-trivial chunk of prose
- `/review-changes` - a more comprehensive fresh-eyes review of both code and prose. The best time to do this is right before declaring a task done, but you can also run it at intermediate checkpoints if you think the set of changes is large enough.

The way to get the most out of these skills is to run them in a loop:

1. Run the skill
2. Triage the findings, deduplicate, discuss
3. Implement if needed
4. Either run the skill again or stop. The signal to stop is when the latest run produced a list of findings that can mostly be ignored (empty, nitpicking, stuff we already dismissed previously). Usually the loop needs to run at least 2 times to get to the 'diminishing returns' signal.
