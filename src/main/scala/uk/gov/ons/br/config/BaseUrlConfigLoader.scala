package uk.gov.ons.br.config


import com.typesafe.config.Config
import com.typesafe.config.ConfigException.BadValue
import play.api.ConfigLoader
import uk.gov.ons.br.utils.{BaseUrl, Port}

import scala.util.control.NonFatal

object BaseUrlConfigLoader extends ConfigLoader[BaseUrl] {
  override def load(rootConfig: Config, path: String): BaseUrl = {
    val config = rootConfig.getConfig(path)
    BaseUrl(
      protocol = config.getString("protocol"),
      host = config.getString("host"),
      port = portFrom(config),
      prefix = noneEmpty(stringOrDefault(config, "prefix"))
    )
  }

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
