# Knowledge Base

What the `knowledge-base/` directory is, how to read it, and the policy for changing it.

## What it is

A small set of canonical documents about this project — the codebase, the domain, and the invariants we maintain. Loaded into the agent's context by default, but written to be useful to humans too.

## Files

- **`knowledge-base/overview.md`** — technical orientation. What the project is, modules, HTTP servers, service wiring, tech stack.
- **`knowledge-base/commands.md`** — commands an agent reaches for while iterating. Compile, test, formatting.
- **`knowledge-base/domain.md`** — domain glossary and main flows.
- **`knowledge-base/styleguide.md`** — coding rules the codebase adheres to. See "Invariants policy" below.
- **`knowledge-base/architecture-principles.md`** — structural rules the codebase adheres to. Same policy as the styleguide.
- **`knowledge-base/patterns/`** — layer-specific reference docs. Read on demand when working in that area.

## Categories of content

Content here is one of three things:

- **Technical info** — commands, folder structure, tech facts.
- **Domain info** — vocabulary, main flows.
- **Invariants** — rules we maintain (style and architecture).

## Core and secondary

Two tiers, by load frequency:

- **Core** — files directly under `knowledge-base/`. Loaded into the agent's context every session via `@` imports in `CLAUDE.md`. Short and broadly relevant; if a doc gets long or only matters to one layer, it isn't core.
- **Secondary** — files in subdirectories of `knowledge-base/` (currently just `patterns/`). Not loaded by default; an agent reads them on demand when working in the relevant area.

The two axes — content type and load frequency — are independent. A pattern doc can be technical (e.g. `patterns/service-layer.md`); an invariants doc could in principle be secondary if it only applies to one area.

## Invariants policy

Adding a rule to `knowledge-base/styleguide.md` or `knowledge-base/architecture-principles.md` means the codebase complies. Every existing violation is one of:

- **fixed** — no marker, the code complies
- **FIXME** — intent to fix; marker stays until the fix lands
- **WONTFIX** — won't fix, with reason (legacy too painful, slated for deletion, etc.)

Inline markers reference the rule's slug:

```scala
// styleguide: no-throw FIXME — see TICKET-XXX
```

A rule is ready to graduate into the directory once the codebase has zero unmarked violations of it. New violations of a graduated rule are not landed.
