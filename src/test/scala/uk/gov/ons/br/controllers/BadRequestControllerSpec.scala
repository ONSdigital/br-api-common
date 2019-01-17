package uk.gov.ons.br.controllers

import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubControllerComponentsFactory}
import uk.gov.ons.br.controllers.BadRequestControllerSpec.UnusedPathParameter
import uk.gov.ons.br.test.UnitSpec

class BadRequestControllerSpec extends UnitSpec {

  private trait Fixture extends StubControllerComponentsFactory {
    val underTest = new BadRequestController(stubControllerComponents())
  }

  "A request" - {
    "receives a Bad Request response" in new Fixture {
      val response = underTest.badRequest(UnusedPathParameter)(FakeRequest())

      status(response) shouldBe BAD_REQUEST
    }
  }
}

object BadRequestControllerSpec {
  val UnusedPathParameter = "unused"
}