# Smart Constructors

A constructor that enforces invariants. Producing an instance is the only way to assert "this value is valid", and the constructor is the only place that assertion can fail.

Used wherever a value has constraints (`Email` is well-formed; `PositiveInt` is positive). For unvalidated id wrapping use `Newtype` instead (see [`newtypes.md`](newtypes.md)).

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
- **`abstract`** — suppresses `copy()` auto-generation. Even with a private constructor, the case class's auto-generated `copy()` stays public — any caller could do `email.copy(value = "anything")` and bypass validation. `abstract` suppresses it.

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

`Either[Err, T]` or `Option[T]` are acceptable when the validation is effect-free; `AppIO[T]` is the default.

## Canonicalization

Smart constructors usually normalize as well as validate. `Email` lowercases and trims; `Phone` parses to E.164 format; a `URL` might canonicalize the trailing slash. Two inputs that should be equal land `==` after construction.

Failure to normalize creates silently context-dependent equality bugs.

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

Two paths to the same validated value — the domain `apply` and the boundary decoder. They can drift if both grow. Extract a private helper if the rule is more than a one-line predicate; accept the duplication for short ones.

## Where these live

Value objects live with their owning bounded context — `AddressLine`, `City`, `PostalCode` are in `customer/domain/model/` because only the customer ctx holds values of those types. The same goes for the validation error they raise (`InvalidAddressLineError`, etc.) and the `CustomerErrorReason` cases (`InvalidAddressLine`, …) that tag them.

Some value objects are shared — a single type that more than one ctx legitimately holds values of. `Email` and `CustomerName` are examples: customer's `Customer` entity holds them, and notification's recipient projection holds a customer-derived view of them. Shared value objects live in `lib/common/domain/model/`, next to `NewTypes.scala`. Their validation errors live under `lib/common/domain/error/domain/` and are tagged with `ErrorCategory.Domain`. `DomainError` is the abstract base; concrete errors map to 400 via `HttpBadRequest`.

The criterion is "more than one ctx legitimately holds values of this type." Speculative sharing (lifting a value object up because *some* future ctx might want it) belongs in the owning ctx until the second consumer is real.

## Background

- [Tuleism on Scala smart constructors](https://tuleism.github.io/blog/2020/scala-smart-constructors/) — covers each technique and the cross-version variations
- [tpolecat's gist](https://gist.github.com/tpolecat/a5cb0dc9adeacc93f846835ed21c92d2) — the canonical recipe, condensed
