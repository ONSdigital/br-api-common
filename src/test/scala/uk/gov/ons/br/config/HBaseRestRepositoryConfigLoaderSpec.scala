package uk.gov.ons.br.config


import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import uk.gov.ons.br.config.HBaseRestRepositoryConfigLoaderSpec.Example
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepositoryConfig
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.utils.{BaseUrl, Port}

class HBaseRestRepositoryConfigLoaderSpec extends UnitSpec {

  private trait Fixture {
    val TargetPath = "db.hbase"

    private def string(value: String): String =
      s""""$value""""

    private def number(value: Long): String =
      value.toString

    def sampleConfigurationWith(prefix: Option[String] = None): String = {
      val defaultValues = Map(
        "username" -> string(Example.Username),
        "password" -> string(Example.Password),
        "timeout" -> number(Example.Timeout),
        "namespace" -> string(Example.Namespace),
        "protocol" -> string(Example.Protocol),
        "host" -> string(Example.Host),
        "port" -> number(Example.Port),
        "tableName" -> string(Example.TableName)
      )

      prefix.fold(defaultValues) { prefixValue =>
        defaultValues + (("prefix", string(prefixValue)))
      }.map { case (k, v) =>
          s"$k = $v"
      }.mkString(s"$TargetPath {", "\n", "}")
    }

    val configOf: String => Config =
      ConfigFactory.parseString(_: String)
  }

  "The configuration of a HBase REST repository" - {
    "can be successfully loaded when" - {
      "the configuration defines a non-empty prefix value" in new Fixture {
        val config = configOf(sampleConfigurationWith(prefix = Some(Example.Prefix)))

        HBaseRestRepositoryConfigLoader.load(rootConfig = config, path = TargetPath) shouldBe HBaseRestRepositoryConfig(
          baseUrl = BaseUrl(protocol = Example.Protocol, host = Example.Host, port = Port(Example.Port), prefix = Some(Example.Prefix)),
          namespace = Example.Namespace,
          tableName = Example.TableName,
          username = Example.Username,
          password = Example.Password,
          timeout = Example.Timeout
        )
      }

      "the configuration defines an empty prefix value" in new Fixture {
        val config = configOf(sampleConfigurationWith(prefix = Some("  ")))

        HBaseRestRepositoryConfigLoader.load(rootConfig = config, path = TargetPath) shouldBe HBaseRestRepositoryConfig(
          baseUrl = BaseUrl(protocol = Example.Protocol, host = Example.Host, port = Port(Example.Port), prefix = None),
          namespace = Example.Namespace,
          tableName = Example.TableName,
          username = Example.Username,
          password = Example.Password,
          timeout = Example.Timeout
        )
      }

      "the configuration omits a prefix value" in new Fixture {
        val config = configOf(sampleConfigurationWith(prefix = None))

        HBaseRestRepositoryConfigLoader.load(rootConfig = config, path = TargetPath) shouldBe HBaseRestRepositoryConfig(
          baseUrl = BaseUrl(protocol = Example.Protocol, host = Example.Host, port = Port(Example.Port), prefix = None),
          namespace = Example.Namespace,
          tableName = Example.TableName,
          username = Example.Username,
          password = Example.Password,
          timeout = 6000L
        )
      }
    }

    "cannot be loaded" - {
      "when a mandatory key is missing" in new Fixture {
        val mandatoryKeys = Seq("username", "password", "timeout", "namespace", "tableName", "protocol", "host", "port")
        mandatoryKeys.foreach { key =>
          withClue(s"with missing key $key") {
            val config = configOf(sampleConfigurationWith(prefix = None).replaceFirst(key, "missing"))
            a [ConfigException] should be thrownBy {
              HBaseRestRepositoryConfigLoader.load(rootConfig = config, path = TargetPath)
            }
          }
        }
      }

      "when the configured timeout value is non-numeric" in new Fixture {
        val badConfig = configOf(sampleConfigurationWith(prefix = None).replaceFirst(s"timeout = ${Example.Timeout}", """timeout = "not-a-number""""))

        a [ConfigException] should be thrownBy {
          HBaseRestRepositoryConfigLoader.load(rootConfig = badConfig, path = TargetPath)
        }
      }

      "when the configured port value" - {
        "is non-numeric" in new Fixture {
          val badConfig = configOf(sampleConfigurationWith(prefix = None).replaceFirst(s"port = ${Example.Port}", """port = "not-a-number""""))

          a [ConfigException] should be thrownBy {
            HBaseRestRepositoryConfigLoader.load(rootConfig = badConfig, path = TargetPath)
          }
        }

        "is negative" in new Fixture {
          val badConfig = configOf(sampleConfigurationWith(prefix = None).replaceFirst(s"port = ${Example.Port}", """port = -1"""))

          a [ConfigException] should be thrownBy {
            HBaseRestRepositoryConfigLoader.load(rootConfig = badConfig, path = TargetPath)
          }
        }

        "is too large" in new Fixture {
          val badConfig = configOf(sampleConfigurationWith(prefix = None).replaceFirst(s"port = ${Example.Port}", """port = 65536"""))

          a [ConfigException] should be thrownBy {
            HBaseRestRepositoryConfigLoader.load(rootConfig = badConfig, path = TargetPath)
          }
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
  }
}