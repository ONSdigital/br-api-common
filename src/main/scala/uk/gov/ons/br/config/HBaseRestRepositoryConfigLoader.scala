package uk.gov.ons.br.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigException.BadValue
import play.api.ConfigLoader
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepositoryConfig
import uk.gov.ons.br.utils.{BaseUrl, Port}

import scala.util.control.NonFatal

/*
 * We want a misconfigured server to "fail fast".
 * A Guice module should be configured to use this ConfigLoader during its configure method.
 * If any required key is missing / any value cannot be successfully parsed, an exception should be thrown
 * which will fail the startup of the service (at deployment time).
 *
 * Note that in real deployment environments the HBase url requires a 'prefix' value that follows the port.
 * This is not the case in local development environments.
 */
object HBaseRestRepositoryConfigLoader extends ConfigLoader[HBaseRestRepositoryConfig] {
  override def load(rootConfig: Config, path: String): HBaseRestRepositoryConfig = {
    val config = rootConfig.getConfig(path)
    HBaseRestRepositoryConfig(
      baseUrl = baseUrlFrom(config),
      namespace = config.getString("namespace"),
      tableName = config.getString("tableName"),
      username = config.getString("username"),
      password = config.getString("password"),
      timeout = config.getLong("timeout")
    )
  }

  private def baseUrlFrom(config: Config): BaseUrl =
    BaseUrl(
      config.getString("protocol"),
      config.getString("host"),
      portFrom(config),
      noneEmpty(stringOrDefault(config, "prefix"))
    )

  private def portFrom(config: Config): Port = {
    val path = "port"
    val n = config.getInt(path)
    try Port(n)
    catch {
      case NonFatal(e) => throw new BadValue(path, e.getMessage)
    }
  }

  private def stringOrDefault(config: Config, key: String, default: => String = ""): String =
    if (config.hasPath(key)) config.getString(key) else default

  private def noneEmpty(str: String): Option[String] =
    if (str.trim.isEmpty) None else Some(str)
}