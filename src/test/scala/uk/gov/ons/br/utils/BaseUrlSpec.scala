package uk.gov.ons.br.utils

import org.scalatest.{FreeSpec, Matchers}
import uk.gov.ons.br.utils.BaseUrl.asUrlString

class BaseUrlSpec extends FreeSpec with Matchers {

  "A port number" - {
    "cannot be negative" in {
      val thrown = the [IllegalArgumentException] thrownBy Port(-1)

      thrown.getMessage should endWith ("Port [-1] is outside the valid range [0-65535]")
    }

    "cannot be greater than 65535" in {
      val thrown = the [IllegalArgumentException] thrownBy Port(65536)

      thrown.getMessage should endWith ("Port [65536] is outside the valid range [0-65535]")
    }

    // reserved under TCP but we will allow such a configuration
    "can be zero" in {
      Port(0).value shouldBe 0
    }

    "can be 65535" in {
      Port(65535).value shouldBe 65535
    }
  }

  "A BaseUrl" - {
    "can be represented as a URL string" - {
      "when it does not contain a prefix component" in {
        asUrlString(BaseUrl(protocol = "http", host = "hostname", port = Port(1234))) shouldBe "http://hostname:1234"
      }

      "when it contains a prefix component" in {
        asUrlString(BaseUrl(protocol = "http", host = "hostname", port = Port(1234), prefix = Some("HBase"))) shouldBe "http://hostname:1234/HBase"
      }
    }
  }
}
