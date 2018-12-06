package uk.gov.ons.br.models

import play.api.libs.json.Json
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.json.JsonString
import uk.gov.ons.br.test.json.JsonString.{withOptionalString, withString}

class LifespanSpec extends UnitSpec {

  private trait Fixture {
    def expectedJsonStrOf(lifespan: Lifespan): String =
      JsonString.ofObject(
        withString(named = "birthDate", withValue = lifespan.birthDate),
        withOptionalString(named = "deathDate", withValue = lifespan.deathDate),
        withOptionalString(named = "deathCode", withValue = lifespan.deathCode)
      )
  }

  "A Lifespan" - {
    "can be represented as Json" - {
      "when all fields are defined" in new Fixture {
        val aLifespan = Lifespan(birthDate = "23/12/2012", deathDate = Some("17/10/2015"), deathCode = Some("658664"))

        Json.toJson(aLifespan) shouldBe Json.parse(expectedJsonStrOf(aLifespan))
      }

      "when only mandatory fields are defined" in new Fixture {
        val aLifespan = Lifespan(birthDate = "23/12/2012", deathDate = None, deathCode = None)

        Json.toJson(aLifespan) shouldBe Json.parse(expectedJsonStrOf(aLifespan))
      }
    }
  }
}
