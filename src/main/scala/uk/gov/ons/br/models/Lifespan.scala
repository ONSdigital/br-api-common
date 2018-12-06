package uk.gov.ons.br.models


import play.api.libs.json.{Json, Writes}

/*
 * Modelling the dates as strings for now - these are a pass-through and so we cannot vouch for their validity.
 */
case class Lifespan(birthDate: String,
                    deathDate: Option[String],
                    deathCode: Option[String])

object Lifespan {
  implicit val writes: Writes[Lifespan] = Json.writes[Lifespan]
}