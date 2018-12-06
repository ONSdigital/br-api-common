package uk.gov.ons.br.models

import play.api.libs.json.{Json, Writes}

/*
 * Modelling ubrn as a simple String here for now.
 * Would prefer a type - but it is not clear if there will be a suitable anti-corruption layer to enforce its invariants.
 */
case class LinkToLegalUnit(ubrn: String)

object LinkToLegalUnit {
  implicit val writes: Writes[LinkToLegalUnit] = Json.writes[LinkToLegalUnit]
}
