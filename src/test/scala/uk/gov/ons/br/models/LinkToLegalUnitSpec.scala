package uk.gov.ons.br.models

import play.api.libs.json.Json
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.json.JsonString
import uk.gov.ons.br.test.json.JsonString.withString

class LinkToLegalUnitSpec extends UnitSpec {

  private trait Fixture {
    def expectedJsonStrOf(link: LinkToLegalUnit): String =
      JsonString.ofObject(
        withString(named = "ubrn", withValue = link.ubrn)
      )
  }

  "A link to a Legal Unit" - {
    "can be represented as Json" in new Fixture {
      val aLink = LinkToLegalUnit(ubrn = "1234567890123456")

      Json.toJson(aLink) shouldBe Json.parse(expectedJsonStrOf(aLink))
    }
  }
}
