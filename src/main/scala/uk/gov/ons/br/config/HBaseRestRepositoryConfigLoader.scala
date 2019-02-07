package uk.gov.ons.br.config


import com.typesafe.config.Config
import javax.inject.Inject
import play.api.ConfigLoader
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepositoryConfig
import uk.gov.ons.br.utils.BaseUrl

/*
 * We want a misconfigured server to "fail fast".
 * A Guice module should be configured to use this ConfigLoader during its configure method.
 * If any required key is missing / any value cannot be successfully parsed, an exception should be thrown
 * which will fail the startup of the service (at deployment time).
 *
 * Note that in real deployment environments the HBase url requires a 'prefix' value that follows the port.
 * This is not the case in local development environments.
 */
class HBaseRestRepositoryConfigLoader @Inject() (baseUrlConfigLoader: ConfigLoader[BaseUrl]) extends ConfigLoader[HBaseRestRepositoryConfig] {
  override def load(rootConfig: Config, path: String): HBaseRestRepositoryConfig = {
    val config = rootConfig.getConfig(path)
    HBaseRestRepositoryConfig(
      baseUrl = baseUrlConfigLoader.load(rootConfig, path),
      namespace = config.getString("namespace"),
      tableName = config.getString("tableName"),
      username = config.getString("username"),
      password = config.getString("password"),
      timeout = config.getLong("timeout")
    )
  }
}

object HBaseRestRepositoryConfigLoader {
  @deprecated(message = "Create a suitable instance of the class rather than using the companion object", since = "1.4")
  def load(rootConfig: Config, path: String): HBaseRestRepositoryConfig = {
    val instance = new HBaseRestRepositoryConfigLoader(BaseUrlConfigLoader)
    instance.load(rootConfig, path)
  }
}