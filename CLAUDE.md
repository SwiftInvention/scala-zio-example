# CLAUDE.md

Here's some general info on this project:

@docs/knowledge-base/overview.md
@docs/knowledge-base/commands.md
@docs/knowledge-base/domain.md
@docs/knowledge-base/styleguide.md
@docs/knowledge-base/architecture-principles.md
@docs/knowledge-base/patterns/local-reasoning.md

There are also specific docs in `docs/knowledge-base/patterns/` that are not in context by default - consider reading some of those when you're working on a relevant area.

We always have `patterns/local-reasoning.md` in the context because it's foundational philosophy we want to keep in mind at all times. The hardest violations to self-catch are the ones rooted in temporal locality: when you edit code or docs you do it while remembering the previous state, and that history leaks into the writing as transitional framing ("now", "no longer", "we used to") that reads natural to you and disorienting to anyone else. Vestigial code shapes (a `Map` that holds one key, an `Option` that's always `Some`, a sealed trait with one case) are the same family of problem.

The counterweight: before declaring a multi-file change finished, run `/review-changes` to dispatch a fresh-eyes sweep against `local-reasoning.md`. Show the findings verbatim before fixing — soft ones have legitimate defenses worth discussing.
