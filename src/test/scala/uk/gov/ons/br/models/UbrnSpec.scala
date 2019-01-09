package uk.gov.ons.br.models

import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess}
import uk.gov.ons.br.test.UnitSpec

class UbrnSpec extends UnitSpec {

  "A UBRN" - {
    "can be created from a value comprising of 16 digits" in {
      Ubrn("1234567890123456").value shouldBe "1234567890123456"
    }

    "cannot be created" - {
      "from a value that contains a non-digit character" in {
        an [IllegalArgumentException] shouldBe thrownBy {
          Ubrn("123456789X123456")
        }
      }

      "from a value with fewer than 16 digits" in {
        an [IllegalArgumentException] shouldBe thrownBy {
          Ubrn("123456789012345")
        }
      }

      "from a value with more than 16 digits" in {
        an [IllegalArgumentException] shouldBe thrownBy {
          Ubrn("12345678901234567")
        }
      }
    }

    "can be read from a Json String comprising of 16 digits" in {
      JsString("1234567890123456").validate[Ubrn] shouldBe JsSuccess(Ubrn("1234567890123456"))
    }

    "cannot be read" - {
      "from a Json String containing a non-digit character" in {
        JsString("123456789X123456").validate[Ubrn] shouldBe a [JsError]
      }

      "from a Json String with fewer than 16 digits" in {
        JsString("123456789012345").validate[Ubrn] shouldBe a [JsError]
      }

      "from a Json String with more than 16 digits" in {
        JsString("12345678901234567").validate[Ubrn] shouldBe a [JsError]
      }

      "from a Json value that is not a string" in {
        JsNumber(12345678901234567L).validate[Ubrn] shouldBe a [JsError]
      }
    }
  }
}
