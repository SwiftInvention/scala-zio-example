package com.example.ctx.customer.domain.model

import com.example.lib.common.domain.model.NewTypes.AddressId
import zio.test._

/** zio-test generators for `Address` and its parts.
  *
  * Each value-object generator emits inputs that satisfy the smart constructor's invariants by construction (length
  * within `[MinLength, MaxLength]`, alphanumeric content). Failures inside `Gen.fromZIO` are `.orDie` because they
  * indicate a generator bug — the constructor should never reject what the generator produced.
  */
object AddressGen {

  val addressIdGen: Gen[Any, AddressId] =
    Gen.alphaNumericStringBounded(min = 1, max = 30).map(AddressId(_))

  val addressLineGen: Gen[Any, AddressLine] =
    Gen
      .alphaNumericStringBounded(min = AddressLine.MinLength, max = AddressLine.MaxLength)
      .flatMap(s => Gen.fromZIO(AddressLine(s).orDie))

  val cityGen: Gen[Any, City] =
    Gen
      .alphaNumericStringBounded(min = City.MinLength, max = City.MaxLength)
      .flatMap(s => Gen.fromZIO(City(s).orDie))

  val postalCodeGen: Gen[Any, PostalCode] =
    Gen
      .alphaNumericStringBounded(min = PostalCode.MinLength, max = PostalCode.MaxLength)
      .flatMap(s => Gen.fromZIO(PostalCode(s).orDie))

  val addressGen: Gen[Any, Address] =
    for {
      id         <- addressIdGen
      customerId <- CustomerGen.customerIdGen
      line       <- addressLineGen
      city       <- cityGen
      postalCode <- postalCodeGen
    } yield Address(id = id, customerId = customerId, line = line, city = city, postalCode = postalCode)
}
