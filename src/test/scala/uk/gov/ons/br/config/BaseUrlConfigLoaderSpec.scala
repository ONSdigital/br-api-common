package uk.gov.ons.br.config


import com.typesafe.config.{ConfigException, ConfigFactory}
import uk.gov.ons.br.config.BaseUrlConfigLoaderSpec.{BaseUrlPath, ConfigStrNoPrefix, Missing, PortValue}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.utils.{BaseUrl, Port}

class BaseUrlConfigLoaderSpec extends UnitSpec {

  "The config for a base URL" - {
    "can be successfully loaded when valid and existing at the specified path" - {
      "when no prefix is defined" in {
        BaseUrlConfigLoader.load(ConfigFactory.parseString(ConfigStrNoPrefix), BaseUrlPath) shouldBe
          BaseUrl(protocol = "http", host = "localhost", port = Port(4567), prefix = None)
      }

      "when a blank prefix is defined" in {
        val configStr =
          """
            |data {
            |  unit {
            |    protocol = "http"
            |    host = "localhost"
            |    port = 4567
            |    prefix = "  "
            |  }
            |}""".stripMargin

        BaseUrlConfigLoader.load(ConfigFactory.parseString(configStr), BaseUrlPath) shouldBe
          BaseUrl(protocol = "http", host = "localhost", port = Port(4567), prefix = None)
      }

      "when a non-blank prefix is defined" in {
        val configStr =
          """
            |data {
            |  unit {
            |    protocol = "http"
            |    host = "localhost"
            |    port = 4567
            |    prefix = "abc"
            |  }
            |}""".stripMargin

        BaseUrlConfigLoader.load(ConfigFactory.parseString(configStr), BaseUrlPath) shouldBe
          BaseUrl(protocol = "http", host = "localhost", port = Port(4567), prefix = Some("abc"))
      }
    }

    "cannot be loaded" - {
      "when it does not exist at the specified path" in {
        a [ConfigException] shouldBe thrownBy {
          BaseUrlConfigLoader.load(ConfigFactory.parseString(ConfigStrNoPrefix), "data")
        }
      }

      "when the config is invalid" - {
        "because the protocol is missing" in {
          val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace("protocol", Missing))

          a [ConfigException] shouldBe thrownBy {
            BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
          }
        }

        "because the host is missing" in {
          val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace("host", Missing))

          a [ConfigException] shouldBe thrownBy {
            BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
          }
        }

        "because the port" - {
          "is missing" in {
            val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace("port", Missing))

            a [ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }

          "is non-numeric" in {
            val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace(PortValue, "fourfivesixseven"))

            a [ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }

          "is negative" in {
            val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace(PortValue, "-1"))

            a [ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }

          "is too large" in {
            val badConfig = ConfigFactory.parseString(ConfigStrNoPrefix.replace(PortValue, "65536"))

            a [ConfigException] shouldBe thrownBy {
              BaseUrlConfigLoader.load(badConfig, BaseUrlPath)
            }
          }
        }
      }
    }
  }
}

private object BaseUrlConfigLoaderSpec {
  val BaseUrlPath = "data.unit"
  val Missing = "missing"
  val PortValue = "4567"
  val ConfigStrNoPrefix =
    s"""
      |data {
      |  unit {
      |    protocol = "http"
      |    host = "localhost"
      |    port = $PortValue
      |  }
      |}""".stripMargin
}