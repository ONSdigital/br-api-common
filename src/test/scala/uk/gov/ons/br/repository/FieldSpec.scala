package uk.gov.ons.br.repository


import org.scalamock.scalatest.MockFactory
import org.slf4j.event.Level.ERROR
import uk.gov.ons.br.repository.Field.{Raw, Typed}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.logging.{LogEntry, LoggingSpy}

import scala.util.{Failure, Success}

/*
 * Note that we use a LoggingSpy rather than a mock logger.
 * This allows us to assert on the semantic content of the log rather than the details of how the log statement was
 * created from a format and variables.
 */
class FieldSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    val Employees = "employees"
    implicit val logger = LoggingSpy.error()
  }

  private trait PresentFixture extends Fixture {
    val EmployeesValue = "42"
    val Variables = Map(Employees -> EmployeesValue)
  }

  private trait MissingFixture extends Fixture {
    val Variables = Map("jobs" -> "36")
  }

  private trait NonNumericFixture extends Fixture {
    val EmployeesValue = "non-numeric"
    val Variables = Map(Employees -> EmployeesValue)
  }

  "A raw field" - {
    "can be extracted from row variables by name" - {
      "when present" in new PresentFixture {
        Raw.named(Employees).apply(Variables) shouldBe Employees -> Some(EmployeesValue)
      }

      "when missing" in new MissingFixture {
        Raw.named(Employees).apply(Variables) shouldBe Employees -> None
      }
    }

    "when optional" - {
      "returns Some(string) when present" in new PresentFixture {
        Raw.optional(Employees -> Some(EmployeesValue)) shouldBe Some(EmployeesValue)
      }

      "returns None when missing" in new MissingFixture {
        Raw.optional(Employees -> None) shouldBe None
      }
    }

    "when mandatory" - {
      "returns Some(string) when present" in new PresentFixture {
        Raw.mandatory.apply(Employees -> Some(EmployeesValue)) shouldBe Some(EmployeesValue)
      }

      "logs when missing" in new MissingFixture {
        Raw.mandatory.apply(Employees -> None) shouldBe None

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Mandatory field [$Employees] is missing.", t = None)
        )
      }
    }
  }

  "A typed field" - {
    "can be obtained by converting a raw field" - {
      "when present and the conversion is successful" in new PresentFixture {
        val successfulConversion = Success(123456L)

        Typed.tryConversion(_ => successfulConversion).apply(Employees -> Some(EmployeesValue)) shouldBe
          Employees -> Some(successfulConversion)
      }

      "when present and the conversion fails" in new PresentFixture {
        val failedConversion = Failure(new Exception("conversion failed"))

        Typed.tryConversion(_ => failedConversion).apply(Employees -> Some(EmployeesValue)) shouldBe
          Employees -> Some(failedConversion)
      }

      "when missing" in new Fixture {
        Typed.tryConversion(_ => Success(123456L)).apply(Employees -> None) shouldBe
          Employees -> None
      }
    }

    "when optional" - {
      "returns Success(Some(value)) when present and a successful conversion" in new Fixture {
        Typed.optional[Int].apply(Employees -> Some(Success(42))) shouldBe Success(Some(42))
      }

      "logs when present and a failed conversion" in new Fixture {
        val cause = new Exception("conversion failed")

        Typed.optional[Int].apply(Employees -> Some(Failure(cause))) shouldBe Failure(cause)

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Conversion attempt of field [$Employees] failed with cause [conversion failed].", t = Some(cause))
        )
      }

      "returns Success(None) when missing" in new Fixture {
        Typed.optional[Int].apply(Employees -> None) shouldBe Success(None)
      }
    }

    "when mandatory" - {
      "returns Success(value) when present and a successful conversion" in new Fixture {
        Typed.mandatory[Int].apply(Employees -> Some(Success(42))) shouldBe Success(42)
      }

      "logs when present and a failed conversion" in new Fixture {
        val cause = new Exception("conversion failed")

        Typed.mandatory[Int].apply(Employees -> Some(Failure(cause))) shouldBe Failure(cause)

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Conversion attempt of field [$Employees] failed with cause [conversion failed].", t = Some(cause))
        )
      }

      "logs when missing" in new Fixture {
        Typed.mandatory[Int].apply(Employees -> None) shouldBe a [Failure[_]]

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Mandatory field [$Employees] is missing.", t = None)
        )
      }
    }
  }

  "A conversion" - {
    "toInt" - {
      "succeeds when the field value represents a valid Int" in new PresentFixture {
        Field.Conversions.toInt(EmployeesValue) shouldBe Success(EmployeesValue.toInt)
      }

      "fails when the field value does not represent a valid Int" in new NonNumericFixture {
        Field.Conversions.toInt(EmployeesValue) shouldBe a [Failure[_]]
      }
    }

    "toBigDecimal" - {
      "succeeds when the field value represents a valid decimal" in new Fixture {
        Field.Conversions.toBigDecimal("3.14159") shouldBe Success(BigDecimal("3.14159"))
      }

      "fails when the field value does not represent a valid decimal" in new NonNumericFixture {
        Field.Conversions.toInt(EmployeesValue) shouldBe a [Failure[_]]
      }
    }
  }

  "A field that is" - {
    "an optionalStringNamed" - {
      "returns Some(string) when present" in new PresentFixture {
        Field.optionalStringNamed(Employees).apply(Variables) shouldBe Some(EmployeesValue)
      }

      "returns None when missing" in new MissingFixture {
        Field.optionalStringNamed(Employees).apply(Variables) shouldBe None
      }
    }

    "a mandatoryStringNamed" - {
      "returns Some(string when present)" in new PresentFixture {
        Field.mandatoryStringNamed(Employees).apply(Variables) shouldBe Some(EmployeesValue)
      }

      "logs when missing" in new MissingFixture {
        Field.mandatoryStringNamed(Employees).apply(Variables) shouldBe None

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Mandatory field [$Employees] is missing.", t = None)
        )
      }
    }

    "an optionalIntNamed" - {
      "returns Success(Some(int)) when a value is present which represents a valid Int" in new PresentFixture {
        Field.optionalIntNamed(Employees).apply(Variables) shouldBe Success(Some(EmployeesValue.toInt))
      }

      "returns Success(None) when missing" in new MissingFixture {
        Field.optionalIntNamed(Employees).apply(Variables) shouldBe Success(None)
      }

      "logs when a value is present which does not represent a valid Int" in new NonNumericFixture {
        Field.optionalIntNamed(Employees).apply(Variables) shouldBe a [Failure[_]]

        logger.log should have size 1
        logger.log.get(0).level shouldBe ERROR
        logger.log.get(0).msg should startWith (s"Conversion attempt of field [$Employees] failed")
      }
    }

    "a mandatoryIntNamed" - {
      "returns Success(int) when a value is present which represents a valid Int" in new PresentFixture {
        Field.mandatoryIntNamed(Employees).apply(Variables) shouldBe Success(EmployeesValue.toInt)
      }

      "logs when missing" in new MissingFixture {
        Field.mandatoryIntNamed(Employees).apply(Variables) shouldBe a [Failure[_]]

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Mandatory field [$Employees] is missing.", t = None)
        )
      }

      "logs when a value is present which does not represent a valid Int" in new NonNumericFixture {
        Field.mandatoryIntNamed(Employees).apply(Variables) shouldBe a [Failure[_]]

        logger.log should have size 1
        logger.log.get(0).level shouldBe ERROR
        logger.log.get(0).msg should startWith (s"Conversion attempt of field [$Employees] failed")
      }
    }

    "a mandatoryBigDecimalNamed" - {
      "returns Success(bigDecimal) when a value is present which represents a valid decimal" in new Fixture {
        Field.mandatoryBigDecimalNamed("pi").apply(Map("pi" -> "3.14159")) shouldBe Success(BigDecimal("3.14159"))
      }

      "logs when missing" in new MissingFixture {
        Field.mandatoryBigDecimalNamed(Employees).apply(Variables) shouldBe a[Failure[_]]

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Mandatory field [$Employees] is missing.", t = None)
        )
      }

      "logs when a value is present which does not represent a valid decimal" in new NonNumericFixture {
        Field.mandatoryBigDecimalNamed(Employees).apply(Variables) shouldBe a [Failure[_]]

        logger.log should have size 1
        logger.log.get(0).level shouldBe ERROR
        logger.log.get(0).msg should startWith (s"Conversion attempt of field [$Employees] failed")
      }
    }

    "an optional object" - {
      "comprising 5 optional fields" - {
        "is not defined when all fields are empty" in new Fixture {
          val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]

          Field.whenExistsNonEmpty(None, None, None, None, None)(fn) shouldBe None
        }

        "is defined when all fields are defined" in new Fixture {
          val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
          fn.expects(Some("a"), Some("b"), Some("c"), Some("d"), Some("e")).returning(42)

          Field.whenExistsNonEmpty(Some("a"), Some("b"), Some("c"), Some("d"), Some("e"))(fn) shouldBe Some(42)
        }

        "is defined when" - {
          "only the first field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(Some("a"), None, None, None, None).returning(1)

            Field.whenExistsNonEmpty(Some("a"), None, None, None, None)(fn) shouldBe Some(1)
          }

          "only the second field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, Some("b"), None, None, None).returning(2)

            Field.whenExistsNonEmpty(None, Some("b"), None, None, None)(fn) shouldBe Some(2)
          }

          "only the third field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, Some("c"), None, None).returning(3)

            Field.whenExistsNonEmpty(None, None, Some("c"), None, None)(fn) shouldBe Some(3)
          }

          "only the fourth field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, None, Some("d"), None).returning(4)

            Field.whenExistsNonEmpty(None, None, None, Some("d"), None)(fn) shouldBe Some(4)
          }

          "only the fifth field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, None, None, Some("e")).returning(5)

            Field.whenExistsNonEmpty(None, None, None, None, Some("e"))(fn) shouldBe Some(5)
          }
        }
      }

      "comprising 6 optional fields" - {
        "is not defined when all fields are empty" in new Fixture {
          val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]

          Field.whenExistsNonEmpty(None, None, None, None, None, None)(fn) shouldBe None
        }

        "is defined when all fields are defined" in new Fixture {
          val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
          fn.expects(Some("a"), Some("b"), Some("c"), Some("d"), Some("e"), Some("f")).returning(42)

          Field.whenExistsNonEmpty(Some("a"), Some("b"), Some("c"), Some("d"), Some("e"), Some("f"))(fn) shouldBe Some(42)
        }

        "is defined when" - {
          "only the first field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(Some("a"), None, None, None, None, None).returning(1)

            Field.whenExistsNonEmpty(Some("a"), None, None, None, None, None)(fn) shouldBe Some(1)
          }

          "only the second field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, Some("b"), None, None, None, None).returning(2)

            Field.whenExistsNonEmpty(None, Some("b"), None, None, None, None)(fn) shouldBe Some(2)
          }

          "only the third field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, Some("c"), None, None, None).returning(3)

            Field.whenExistsNonEmpty(None, None, Some("c"), None, None, None)(fn) shouldBe Some(3)
          }

          "only the fourth field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, None, Some("d"), None, None).returning(4)

            Field.whenExistsNonEmpty(None, None, None, Some("d"), None, None)(fn) shouldBe Some(4)
          }

          "only the fifth field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, None, None, Some("e"), None).returning(5)

            Field.whenExistsNonEmpty(None, None, None, None, Some("e"), None)(fn) shouldBe Some(5)
          }

          "only the sixth field is defined" in new Fixture {
            val fn = mockFunction[Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Int]
            fn.expects(None, None, None, None, None, Some("f")).returning(6)

            Field.whenExistsNonEmpty(None, None, None, None, None, Some("f"))(fn) shouldBe Some(6)
          }
        }
      }
    }

    "an optional concatenated string" - {
      "is not defined when all fields are not defined" in new Fixture {
        Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(Map.empty) shouldBe None
      }

      "concatenates all fields in the order specified when all fields are defined" in new Fixture {
        val variables = Map("aName" -> "aValue", "bName" -> "bValue", "cName" -> "cValue")

        Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
          Some("aValuebValuecValue")
      }

      "comprises" - {
        "only the first field when it is the only field defined" in new Fixture {
          val variables = Map("aName" -> "aValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValue")
        }

        "only the second field when it is the only field defined" in new Fixture {
          val variables = Map("bName" -> "bValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("bValue")
        }

        "only the third field when it is the only field defined" in new Fixture {
          val variables = Map("cName" -> "cValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("cValue")
        }

        "the second field appended to the first when they are the only fields defined" in new Fixture {
          val variables = Map("aName" -> "aValue", "bName" -> "bValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValuebValue")
        }

        "the third field appended to the first when they are the only fields defined" in new Fixture {
          val variables = Map("aName" -> "aValue", "cName" -> "cValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValuecValue")
        }

        "the third field appended to the second when they are the only fields defined" in new Fixture {
          val variables = Map("bName" -> "bValue", "cName" -> "cValue")

          Field.optionalConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("bValuecValue")
        }
      }
    }

    "a mandatory concatenated string" - {
      "is not defined when all fields are not defined" in new Fixture {
        Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(Map.empty) shouldBe None

        logger.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"Unable to concatenate mandatory string.  All of [aName,bName,cName] are missing.", t = None)
        )
      }

      "concatenates all fields in the order specified when all fields are defined" in new Fixture {
        val variables = Map("aName" -> "aValue", "bName" -> "bValue", "cName" -> "cValue")

        Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
          Some("aValuebValuecValue")
      }

      "comprises" - {
        "only the first field when it is the only field defined" in new Fixture {
          val variables = Map("aName" -> "aValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValue")
        }

        "only the second field when it is the only field defined" in new Fixture {
          val variables = Map("bName" -> "bValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("bValue")
        }

        "only the third field when it is the only field defined" in new Fixture {
          val variables = Map("cName" -> "cValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("cValue")
        }

        "the second field appended to the first when they are the only fields defined" in new Fixture {
          val variables = Map("aName" -> "aValue", "bName" -> "bValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValuebValue")
        }

        "the third field appended to the first when they are the only fields defined" in new Fixture {
          val variables = Map("aName" -> "aValue", "cName" -> "cValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("aValuecValue")
        }

        "the third field appended to the second when they are the only fields defined" in new Fixture {
          val variables = Map("bName" -> "bValue", "cName" -> "cValue")

          Field.mandatoryConcatenatedStringFrom("aName", "bName", "cName").apply(variables) shouldBe
            Some("bValuecValue")
        }
      }
    }
  }
}