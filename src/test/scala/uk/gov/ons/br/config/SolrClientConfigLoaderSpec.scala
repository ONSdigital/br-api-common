package uk.gov.ons.br.config


import com.typesafe.config.{ConfigException, ConfigFactory}
import uk.gov.ons.br.test.UnitSpec
import SolrClientConfigLoaderSpec.{Missing, SolrCloudConfig, SolrConfigPath, StandaloneConfig}
import com.typesafe.config.ConfigException.BadValue
import org.scalamock.scalatest.MockFactory
import play.api.ConfigLoader
import uk.gov.ons.br.utils.{BaseUrl, Port}

class SolrClientConfigLoaderSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    val baseUrlConfigLoader = mock[ConfigLoader[BaseUrl]]
    val underTest = new SolrClientConfigLoader(baseUrlConfigLoader)
  }

  "A Solr Client ConfigLoader" - {
    "can successfully load Solr client configuration" - {
      "when the config defines a Solr standalone client" in new Fixture {
        (baseUrlConfigLoader.load _).expects(*, "standalone").returning(
          BaseUrl(protocol = "http", host = "localhost", port = Port(8984), prefix = Some("solr"))
        )

        underTest.load(ConfigFactory.parseString(StandaloneConfig), SolrConfigPath) shouldBe
          StandaloneSolrClientConfig(baseUrl = "http://localhost:8984/solr/unit")
      }

      "when the config defines a Solr Cloud client" - {
        "(with a single zookeeper host)" in new Fixture {
          underTest.load(ConfigFactory.parseString(SolrCloudConfig), SolrConfigPath) shouldBe
            SolrCloudClientConfig(zookeeperHosts = "localhost:9983", collection = "unit")
        }

        "(with multiple zookeeper hosts)" in new Fixture {
          val config =
            """|search {
               |  db {
               |    solr {
               |      collection = "unit"
               |      cloud {
               |        zookeeperHosts = [
               |          "zookeepera:9983",
               |          "zookeeperb:9984"
               |        ]
               |      }
               |    }
               |  }
               |}""".stripMargin

          underTest.load(ConfigFactory.parseString(config), SolrConfigPath) shouldBe
            SolrCloudClientConfig(zookeeperHosts = "zookeepera:9983,zookeeperb:9984", collection = "unit")
        }
      }

      // override behaviour for integration testing
      "when both standalone & Solr Cloud configurations are defined the standalone config is selected" in new Fixture {
        val config =
          """|search {
             |  db {
             |    solr {
             |      collection = "unit"
             |      cloud {
             |        zookeeperHosts = ["localhost:9983"]
             |      }
             |      standalone {
             |        protocol = "http"
             |        host = "localhost"
             |        port = 8984
             |        prefix = "solr"
             |      }
             |    }
             |  }
             |}""".stripMargin
        (baseUrlConfigLoader.load _).expects(*, "standalone").returning(
          BaseUrl(protocol = "http", host = "localhost", port = Port(8984), prefix = Some("solr"))
        )

        underTest.load(ConfigFactory.parseString(config), SolrConfigPath) shouldBe
          StandaloneSolrClientConfig(baseUrl = "http://localhost:8984/solr/unit")
      }
    }

    "cannot load Solr client configuration" - {
      "when the target collection is missing" in new Fixture {
        val badConfig = ConfigFactory.parseString(StandaloneConfig.replace("collection", Missing))

        a [ConfigException] shouldBe thrownBy {
          underTest.load(badConfig, SolrConfigPath)
        }
      }

      "when both the standalone and cloud configurations are missing" in new Fixture {
        val badConfig = ConfigFactory.parseString(StandaloneConfig.replace("standalone", Missing))

        a [ConfigException] shouldBe thrownBy {
          underTest.load(badConfig, SolrConfigPath)
        }
      }

      "when a Solr Cloud configuration" - {
        "is missing a zookeeperHosts configuration" in new Fixture {
          val badConfig = ConfigFactory.parseString(SolrCloudConfig.replace("zookeeperHosts", Missing))

          a [ConfigException] shouldBe thrownBy {
            underTest.load(badConfig, SolrConfigPath)
          }
        }
      }

      "when a Standalone configuration" - {
        "does not define a valid baseUrl" in new Fixture {
          (baseUrlConfigLoader.load _).expects(*, "standalone").throwing(new BadValue("path", "message"))

          a [ConfigException] shouldBe thrownBy {
            underTest.load(ConfigFactory.parseString(StandaloneConfig), SolrConfigPath)
          }
        }
      }
    }
  }
}

private object SolrClientConfigLoaderSpec {
  val Missing = "missing"
  val SolrConfigPath = "search.db.solr"

  val StandaloneConfig =
    """|search {
       |  db {
       |    solr {
       |      collection = "unit"
       |      standalone {
       |        protocol = "http"
       |        host = "localhost"
       |        port = 8984
       |        prefix = "solr"
       |      }
       |    }
       |  }
       |}""".stripMargin

  val SolrCloudConfig =
    """|search {
       |  db {
       |    solr {
       |      collection = "unit"
       |      cloud {
       |        zookeeperHosts = ["localhost:9983"]
       |      }
       |    }
       |  }
       |}""".stripMargin
}