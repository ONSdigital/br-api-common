package uk.gov.ons.br.models

import play.api.libs.json.Reads.StringReads
import play.api.libs.json.{JsResult, JsValue, Reads}
import uk.gov.ons.br.utils.JsResultSupport

import scala.util.Try

// Unique Business Reference Number
// private constructor dictates that an instance is only available via apply() - which applies validation.
final case class Ubrn private (value: String)

object Ubrn {
  def apply(value: String): Ubrn = {
    require(value.matches("\\d{16}"), "Ubrn must be 16 digits")
    new Ubrn(value)
  }

  implicit val reads: Reads[Ubrn] = new Reads[Ubrn] {
    override def reads(json: JsValue): JsResult[Ubrn] =
      StringReads.reads(json).flatMap { str =>
        JsResultSupport.fromTry(Try(Ubrn(str)))
      }
  }
}
