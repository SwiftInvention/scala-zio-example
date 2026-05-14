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

Top-level definitions in `package.scala` files aren't greppable by their declaration site — you can't easily find where `def foo` lives. Use named objects in named files (`Foo.scala`, with `def foo` in it) instead.

## `final-by-default` — concrete classes are `final` unless designed for extension

Non-`final` opens a class to override at a distance: the method body invoked might not be the body read. Default to `final class` and `final case class`. Sealed traits and abstract classes intended as ADT roots or service traits are fine — those are designed for extension.

## `no-default-args` — function parameters don't have default values

Default arguments hide the value at the call site: the reader of `serve(routes)` can't tell whether a port was passed or defaulted. Every caller passes every parameter explicitly — the call site stays honest about what value is in play.

## `named-args` — named arguments at arity ≥ 3, or when two parameters share a type

```scala
// banned
createUser("Ada", "ada@example.test", true, false)

// required
createUser(name = "Ada", email = "ada@example.test", isAdmin = true, isActive = false)
```

Use named args when **(a)** the function takes 3+ parameters, OR **(b)** any two parameters share a type. Clause (b) catches Boolean/Option-blindness below the arity threshold — `transfer(fromId, toId, amount)` with two `CustomerId`s is unsafe at arity 2 even though arity-2 is otherwise positional-fine.

## `no-null` — never use `null`

Absence is `Option[X]`. `null` bypasses the type system's nullability story and is invisible at use sites. Java-API interop is the only legitimate exception, and it gets wrapped at the boundary — callers never see the `null`.

## `no-var` — never use `var`

State is values flowing through transformations, not slots being reassigned. `var` reintroduces the action-at-a-distance and aliasing problems that immutability prevents.

## `http-url-type` — HTTP URLs flow as `zio.http.URL`

When an HTTP/HTTPS URL is the value held across a typed surface — config field, function parameter, return type, case-class field — the type is `zio.http.URL`. Parse once at the boundary that produces the value; downstream code holds `URL` and trusts the type.

Construction goes through `URLHelper` (in `lib/common/.../domain/model/`):

- `URLHelper.parse(s: String): AppIO[URL]` — for runtime call sites where the parse failure should become an `AppFailure`.
- `URLHelper.parseEither(s: String): Either[InvalidURLError, URL]` — for PureConfig `ConfigReader`s and similar `Either`-shaped sinks.
