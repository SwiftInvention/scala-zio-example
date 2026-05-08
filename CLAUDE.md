# CLAUDE.md

Here's some general info on this project:

@docs/knowledge-base/overview.md
@docs/knowledge-base/commands.md
@docs/knowledge-base/domain.md
@docs/knowledge-base/styleguide.md
@docs/knowledge-base/architecture-principles.md
@docs/knowledge-base/patterns/local-reasoning.md

There are also specific docs in `docs/knowledge-base/patterns/` that are not in context by default - consider reading some of those when you're working on a relevant area.
We always have `patterns/local-reasoning.md` in the context because it's foundational philosophy we want to keep in mind at all times.

There are some types of mistakes that are naturally hard to catch for you. One example is temporal locality: you edit code or docs while remembering the previous state, and that history leaks into the writing as transitional framing ("now", "no longer", "we used to") that reads natural to you but disorienting to a cold reader. The counter this and some other blind spots, run `/review-changes` to dispatch a fresh-eyes sweep against your edits. The best time to do this is right before declaring the task done, but you can also run it at intermediate checkpoints if you think the set of changes is large enough.
