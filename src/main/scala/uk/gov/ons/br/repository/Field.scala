package uk.gov.ons.br.repository

import org.slf4j.Logger
import uk.gov.ons.br.repository.Field.Conversions.{toBigDecimal, toInt}

import scala.Function.const
import scala.util.{Failure, Success, Try}

object Field {
  type Variables = Map[String, String]
  type RawField = (String, Option[String])
  type TypedField[A] = (String, Option[Try[A]])

  private[repository] object Raw {
    def named(name: String): Variables => RawField =
      fields => name -> fields.get(name)

    /*
     * End states for untyped (String) fields.
     */

    def optional: RawField => Option[String] = {
      case (_, optValue) => optValue
    }

    def mandatory(implicit logger: Logger): RawField => Option[String] = {
      case (name, optValue) =>
        if (optValue.isEmpty) logMissingMandatoryField(logger, name)
        optValue
    }
  }

  private[repository] object Typed {
    def tryConversion[A](f: String => Try[A]): RawField => TypedField[A] = {
      case (name, optValue) => name -> optValue.map(f)
    }

    /*
     * End states for typed fields.
     */

    def optional[A](implicit logger: Logger): TypedField[A] => Try[Option[A]] =
      endState(
        onMissing = const[Try[Option[A]], String](Success(None)),
        onSuccessfulConversion = Some.apply)

    def mandatory[A](implicit logger: Logger): TypedField[A] => Try[A] =
      endState(
        onMissing = name => {
          logMissingMandatoryField(logger, name)
          Failure(new NoSuchElementException(name))
        },
        onSuccessfulConversion = identity)

    private def endState[A, B](onMissing: String => Try[B], onSuccessfulConversion: A => B)(implicit logger: Logger): TypedField[A] => Try[B] = {
      case (name, optTryValue) =>
        optTryValue.fold(onMissing(name)) { tryValue =>
          logCauseOnFailure(logger, name, tryValue)
          tryValue.map(onSuccessfulConversion)
        }
    }

    private def logCauseOnFailure[A](logger: Logger, name: String, tryValue: Try[A]): Unit =
      tryValue.failed.foreach { cause =>
        logFailedConversion(logger, name, cause)
      }

    private def logFailedConversion(logger: Logger, name: String, cause: Throwable): Unit =
      logger.error(s"Conversion attempt of field [$name] failed with cause [${cause.getMessage}].", cause)
  }

  private[repository] object Conversions {
    def toInt(field: String): Try[Int] =
      Try(field.toInt)

    def toBigDecimal(field: String): Try[BigDecimal] =
      Try(BigDecimal(field))
  }

  private def logMissingMandatoryField(logger: Logger, name: String): Unit =
    logger.error("Mandatory field [{}] is missing.", name)

  /*
   * Both optional & mandatory strings are returned as Option values to support use in for expressions.
   * The only difference between the two is that mandatory will log an error if the field is missing.
   *
   * When used in for expressions:
   * - if the field is optional '=' should be used to capture the value.
   * - if the field is mandatory a generator '<-' should be used to abort the expression
   */
  def optionalStringNamed(name: String): Variables => Option[String] =
    Raw.optional.compose(Raw.named(name))

  def mandatoryStringNamed(name: String)(implicit logger: Logger): Variables => Option[String] =
    Raw.mandatory.compose(Raw.named(name))

  /*
   * Whether the field is optional or mandatory determines the value of the Try when the field is missing.
   * Optional fields default to Success, and mandatory fields default to Failure.
   * This supports use in for expressions, with an optional field yielding None when missing, in contrast to
   * a mandatory field which aborts the expression when missing.
   */
  def optionalIntNamed(name: String)(implicit logger: Logger): Variables => Try[Option[Int]] =
    Typed.optional[Int].compose(anIntNamed(name))

  def mandatoryIntNamed(name: String)(implicit logger: Logger): Variables => Try[Int] =
    Typed.mandatory[Int].compose(anIntNamed(name))

  private def anIntNamed(name: String): Variables => (String, Option[Try[Int]]) =
    Raw.named(name).andThen(Typed.tryConversion(toInt))

  def mandatoryBigDecimalNamed(name: String)(implicit logger: Logger): Variables => Try[BigDecimal] =
    Typed.mandatory[BigDecimal].compose(aBigDecimalNamed(name))

  private def aBigDecimalNamed(name: String): Variables => (String, Option[Try[BigDecimal]]) =
    Raw.named(name).andThen(Typed.tryConversion(toBigDecimal))

  /*
   * This supports the scenario where a field is a sub-object consisting of optional values, and we only want to
   * define the field if at least one of the optional values is non-empty.
   *
   * The second argument-list represents the function to apply, and will typically be the apply function of the case
   * class that represents the field type.
   */
  def whenExistsNonEmpty[A, B, C, D, E, F](a: Option[A], b: Option[B], c: Option[C], d: Option[D], e: Option[E])
                                          (f: (Option[A], Option[B], Option[C], Option[D], Option[E]) => F): Option[F] =
    whenExistsNonEmpty(a, b, c, d, e, None) { (a, b, c, d, e, _) =>
      f(a, b, c, d, e)
    }

  def whenExistsNonEmpty[A, B, C, D, E, F, G](a: Option[A], b: Option[B], c: Option[C], d: Option[D], e: Option[E], f: Option[F])
                                             (g: (Option[A], Option[B], Option[C], Option[D], Option[E], Option[F]) => G): Option[G] =
    a.orElse(b).orElse(c).orElse(d).orElse(e).orElse(f).map { _ =>
      g(a, b, c, d, e, f)
    }

  /*
   * The following cater for the special case where we want to map many database fields to one logical field.
   * This only makes sense for strings - where we simply concatenate the field values.
   */
  def optionalConcatenatedStringFrom(name: String, nextName: String, otherNames: String*): Variables => Option[String] =
    concatenatedStringFrom(name, nextName, otherNames: _*)(None)

  def mandatoryConcatenatedStringFrom(name: String, nextName: String, otherNames: String*)(implicit logger: Logger): Variables => Option[String] =
    concatenatedStringFrom(name, nextName, otherNames: _*) {
      logger.error("Unable to concatenate mandatory string.  All of [{}] are missing.", (name +: nextName +: otherNames).mkString(","))
      None
    }

  // we use this signature to mandate at least two field names
  private def concatenatedStringFrom(name: String, nextName: String, otherNames: String*)
                                    (onNotDefined: => Option[String]): Variables => Option[String] = {
    val fields = (name +: nextName +: otherNames).map(optionalStringNamed)
    variables => {
      val values = fields.map(_.apply(variables))
      if (values.exists(_.isDefined)) Some(values.flatten.mkString) else onNotDefined
    }
  }
}