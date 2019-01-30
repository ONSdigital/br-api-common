package uk.gov.ons.br.config


import com.typesafe.config.ConfigException.BadValue
import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.scalamock.scalatest.MockFactory
import play.api.ConfigLoader
import uk.gov.ons.br.config.HBaseRestRepositoryConfigLoaderSpec.Example
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepositoryConfig
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.utils.BaseUrl

class HBaseRestRepositoryConfigLoaderSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    val TargetPath = "db.hbase"

    private def string(value: String): String =
      s""""$value""""

    private def number(value: Long): String =
      value.toString

    def sampleConfiguration: String = {
      Map(
        "username" -> string(Example.Username),
        "password" -> string(Example.Password),
        "timeout" -> number(Example.Timeout),
        "namespace" -> string(Example.Namespace),
        "protocol" -> string(Example.Protocol),
        "host" -> string(Example.Host),
        "port" -> number(Example.Port),
        "tableName" -> string(Example.TableName),
        "prefix" -> string(Example.Prefix)
      ).map { case (k, v) =>
        s"$k = $v"
      }.mkString(s"$TargetPath {", "\n", "}")
    }

    val configOf: String => Config =
      ConfigFactory.parseString(_: String)

    val baseUrlConfigLoader = mock[ConfigLoader[BaseUrl]]
    val underTest = new HBaseRestRepositoryConfigLoader(baseUrlConfigLoader)
  }

  "The configuration of a HBase REST repository" - {
    "can be successfully loaded when valid" in new Fixture {
      val config = configOf(sampleConfiguration)
      (baseUrlConfigLoader.load _).expects(config, TargetPath).returning(Example.Url)

      underTest.load(rootConfig = config, path = TargetPath) shouldBe HBaseRestRepositoryConfig(
        baseUrl = Example.Url,
        namespace = Example.Namespace,
        tableName = Example.TableName,
        username = Example.Username,
        password = Example.Password,
        timeout = Example.Timeout
      )
    }

    "cannot be loaded" - {
      "when a mandatory key is missing" in new Fixture {
        val mandatoryKeys = Seq("username", "password", "timeout", "namespace", "tableName")
        (baseUrlConfigLoader.load _).expects(*, TargetPath).repeated(mandatoryKeys.size).returning(Example.Url)

        mandatoryKeys.foreach { key =>
          withClue(s"with missing key $key") {
            val config = configOf(sampleConfiguration.replaceFirst(key, "missing"))
            a [ConfigException] should be thrownBy {
              underTest.load(rootConfig = config, path = TargetPath)
            }
          }
        }
      }

      "when the configured timeout value is non-numeric" in new Fixture {
        (baseUrlConfigLoader.load _).expects(*, TargetPath).returning(Example.Url)
        val badConfig = configOf(sampleConfiguration.replaceFirst(
          s"timeout = ${Example.Timeout}", """timeout = "not-a-number"""")
        )

        a [ConfigException] should be thrownBy {
          underTest.load(rootConfig = badConfig, path = TargetPath)
        }
      }

      "when the configured baseUrl is invalid" in new Fixture {
        val config = configOf(sampleConfiguration)
        (baseUrlConfigLoader.load _).expects(config, TargetPath).throwing(new BadValue("path", "message"))

        a [ConfigException] should be thrownBy {
          underTest.load(rootConfig = config, path = TargetPath)
        }
      }
    }
  }
}

private object HBaseRestRepositoryConfigLoaderSpec {
  object Example {
    val Username = "example-username"
    val Password = "example-password"
    val Timeout = 6000L
    val Namespace = "example-namespace"
    val Protocol = "http"
    val Host = "example-hostname"
    val Port = 1234
    val TableName = "example-tablename"
    val Prefix = "example-prefix"
    val Url = BaseUrl(protocol = Protocol, host = Host, port = uk.gov.ons.br.utils.Port(Port), prefix = Some(Prefix))
  }
}