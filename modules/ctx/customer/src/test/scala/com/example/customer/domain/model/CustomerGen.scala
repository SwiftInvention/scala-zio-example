package com.example.customer.domain.model

import com.example.common.domain.model.NewTypes.CustomerId
import zio.test._

/** zio-test generators for `Customer` and its parts.
  *
  * Bounded so generated inputs always satisfy the smart-constructor invariants — `emailGen` only emits well-formed
  * addresses, `customerNameGen` only emits names within `[MinLength, MaxLength]`. Failing the smart constructor inside
  * a Gen would surface as a hung shrink, not a useful failure, so we generate by parts and lift through `Gen.fromZIO`.
  */
object CustomerGen {

  val customerIdGen: Gen[Any, CustomerId] =
    Gen.alphaNumericStringBounded(min = 1, max = 30).map(CustomerId(_))

  val emailGen: Gen[Any, Email] =
    for {
      local  <- Gen.alphaNumericStringBounded(min = 1, max = 20).map(_.toLowerCase)
      domain <- Gen.alphaNumericStringBounded(min = 1, max = 20).map(_.toLowerCase)
      tld    <- Gen.stringN(n = 3)(Gen.alphaChar).map(_.toLowerCase)
      email  <- Gen.fromZIO(Email(s"$local@$domain.$tld").orDie)
    } yield email

  val customerNameGen: Gen[Any, CustomerName] =
    Gen
      .alphaNumericStringBounded(min = CustomerName.MinLength, max = CustomerName.MaxLength)
      .flatMap(s => Gen.fromZIO(CustomerName(s).orDie))

  val customerGen: Gen[Any, Customer] =
    for {
      id    <- customerIdGen
      email <- emailGen
      name  <- customerNameGen
    } yield Customer(id = id, email = email, name = name)
}
