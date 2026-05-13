# Smart Constructors

A constructor that enforces invariants. Producing an instance is the only way to assert "this value is valid", and the constructor is the only place that assertion can fail.

Used wherever a value has constraints (`Email` is well-formed; `PositiveInt` is positive; `Email` also normalizes to lowercase). For unvalidated id wrapping use `Newtype` instead (see [`newtypes.md`](newtypes.md)).

## Canonical idiom

```scala
sealed abstract case class Email private (value: String)
object Email {
  def apply(s: String): AppIO[Email] = {
    val normalized = s.trim.toLowerCase
    if (normalized.matches(emailRegex))
      ZIO.succeed(new Email(normalized) {})
    else
      ZIO.fail(InvalidEmailError(s"Invalid email: '$s'"))
  }
}
```

Each keyword in `sealed abstract case class ... private` defeats a specific Scala-generated escape:

- **`sealed`** — no extension outside the source file. Stops a careless extender from sidestepping invariants via a subclass.
- **`private` constructor** — hides the auto-generated public `apply` in the companion. Without this, callers could do `Email("anything")` via the case class's default machinery.
- **`abstract`** — suppresses both `copy()` and `apply()` auto-generation. In modern Scala (2.12.2+), even with a private constructor, the case class's auto-generated `copy()` stays public — so any caller could do `email.copy(value = "anything")` and bypass validation entirely. `abstract` neutralizes the case-class auto-generation.

Construction goes through `new Email(normalized) {}` — anonymous-subclass syntax — inside the companion. Outside the file, only the validating `apply` is reachable. Pattern matching via `unapply` still works; field access and `equals` are preserved.

## Effect channel

`apply` returns `AppIO[T]` (the codebase's standard effect channel). Failures flow as typed `AppFailure`s through the same channel as everything else, and compose naturally in for-comprehensions:

```scala
for {
  email <- Email(emailRaw)
  phone <- Phone(phoneRaw)
  user  <- userService.create(email, phone)
} yield user
```

If the validation is genuinely effect-free and a simpler signature is preferable, `Either[Err, T]` or `Option[T]` work — but `AppIO[T]` is the default for parity with everything else.

## Canonicalization

Smart constructors usually normalize as well as validate. `Email` lowercases and trims; `Phone` parses to E.164 format; a `URL` might canonicalize the trailing slash. Two inputs that should be equal land `==` after construction.

Failure to normalize creates equality bugs that are silently context-dependent: the same email entered with different casing fails to deduplicate, two phone numbers in different formats won't match a uniqueness check, and so on.

## Codec / decoder boundary

JSON decoders, query-string parsers, and similar boundary parsers want `Either[String, T]` (or specific library shapes), not `AppIO[T]`. The validation logic gets re-expressed there:

```scala
implicit val decoder: Decoder[Email] =
  Decoder.decodeString.emap { raw =>
    val normalized = raw.trim.toLowerCase
    if (normalized.matches(emailRegex)) Right(new Email(normalized) {})
    else Left(s"Invalid email: '$raw'")
  }
```

Two paths to the same validated value — the domain `apply` and the boundary decoder. They can drift if both grow nontrivially. Extract a private helper if the rule is more than a one-line predicate; accept the duplication for short ones.

(The `new Email(...) {}` anonymous-subclass syntax works in the decoder too — it's how the boundary path produces an `Email` without going through the `AppIO`-returning `apply`.)

## Background

- [Tuleism on Scala smart constructors](https://tuleism.github.io/blog/2020/scala-smart-constructors/) — covers each technique and the cross-version variations
- [tpolecat's gist](https://gist.github.com/tpolecat/a5cb0dc9adeacc93f846835ed21c92d2) — the canonical recipe, condensed
