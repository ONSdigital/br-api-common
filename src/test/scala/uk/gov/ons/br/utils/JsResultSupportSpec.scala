package uk.gov.ons.br.utils

import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.ons.br.test.UnitSpec

import scala.util.{Failure, Success}

class JsResultSupportSpec extends UnitSpec {

  "A JsResult" - {
    "can be created from a Try" - {
      "when a Success" in {
        val value = "hello world!"

        JsResultSupport.fromTry(Success(value)) shouldBe JsSuccess(value)
      }

      "when a Failure" in {
        val cause = new Exception("failure message")

        JsResultSupport.fromTry(Failure(cause)) shouldBe JsError(cause.getMessage)
      }
    }
  }
}
