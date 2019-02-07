package uk.gov.ons.br.config


import com.typesafe.config.Config
import javax.inject.Inject
import play.api.ConfigLoader
import uk.gov.ons.br.config.SolrClientConfigLoader.Standalone
import uk.gov.ons.br.utils.BaseUrl


sealed trait SolrClientConfig
case class StandaloneSolrClientConfig(baseUrl: String) extends SolrClientConfig
case class SolrCloudClientConfig(zookeeperHosts: String, collection: String) extends SolrClientConfig

class SolrClientConfigLoader @Inject() (baseUrlConfigLoader: ConfigLoader[BaseUrl]) extends ConfigLoader[SolrClientConfig] {
  override def load(rootConfig: Config, path: String): SolrClientConfig = {
    val config = rootConfig.getConfig(path)
    val collection = config.getString("collection")
    if (config.hasPath(Standalone)) configForStandaloneClient(baseUrlConfigLoader.load(config, Standalone), collection)
    else configForCloudClient(config.getConfig("cloud"), collection)
  }

  private def configForStandaloneClient(baseUrl: BaseUrl, collection: String): StandaloneSolrClientConfig =
    StandaloneSolrClientConfig(BaseUrl.asUrlString(baseUrl) + "/" + collection)

  private def configForCloudClient(config: Config, collection: String): SolrCloudClientConfig = {
    import scala.collection.JavaConverters._
    SolrCloudClientConfig(config.getStringList("zookeeperHosts").asScala.mkString(","), collection)
  }
}

private object SolrClientConfigLoader {
  private val Standalone = "standalone"
}