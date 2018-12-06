package uk.gov.ons.br.models


import play.api.libs.json.Json
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.json.JsonString
import uk.gov.ons.br.test.json.JsonString.{withOptionalString, withString}

class AddressSpec extends UnitSpec {

  private trait Fixture {
    def expectedJsonStrOf(address: Address): String =
      JsonString.ofObject(
        withString(named = "line1", withValue = address.line1),
        withOptionalString(named = "line2", withValue = address.line2),
        withOptionalString(named = "line3", withValue = address.line3),
        withOptionalString(named = "line4", withValue = address.line4),
        withOptionalString(named = "line5", withValue = address.line5),
        withString(named = "postcode", withValue = address.postcode)
      )
  }

  "An Address" - {
    "can be represented as Json" - {
      "when all fields are defined" in new Fixture {
        val anAddress = Address(line1 = "line1-value", line2 = Some("line2-value"), line3 = Some("line3-value"),
          line4 = Some("line4-value"), line5 = Some("line5-value"), postcode = "postcode-value")

        Json.toJson(anAddress) shouldBe Json.parse(expectedJsonStrOf(anAddress))
      }

      "when only mandatory fields are defined" in new Fixture {
        val anAddress = Address(line1 = "line1-value", line2 = None, line3 = None, line4 = None, line5 = None,
          postcode = "postcode-value")

        Json.toJson(anAddress) shouldBe Json.parse(expectedJsonStrOf(anAddress))
      }
    }
  }
}
