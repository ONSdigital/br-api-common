package uk.gov.ons.br.models


import play.api.libs.json.{Json, Writes}

case class Address(line1: String,
                   line2: Option[String],
                   line3: Option[String],
                   line4: Option[String],
                   line5: Option[String],
                   postcode: String)

object Address {
  implicit val writes: Writes[Address] = Json.writes[Address]
}
