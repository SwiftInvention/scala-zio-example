# Style Guide

Rules the codebase adheres to.

Adding a rule here requires that every existing violation is fixed, marked `FIXME` (intent to fix), or marked `WONTFIX` (with reason — legacy too painful, code slated for deletion, etc.). New code introduces no unmarked violations.

Inline markers go next to the offending code, referencing the rule by its slug:

```scala
// styleguide: no-package-files FIXME
```

---

## `impl-suffix` — concrete trait impls use the `Impl` suffix

The default name for a concrete implementation of trait `Foo` is `Foo-ImplKind-Impl`.
`CustomerClient` -> `CustomerClientDirectImpl`
`CustomerRepo` -> `CustomerRepoMySQLImpl`
The companion's layer val is named `layer` (e.g. `CustomerRepoMySQLImpl.layer`).

## `no-package-files` — don't use package.scala
