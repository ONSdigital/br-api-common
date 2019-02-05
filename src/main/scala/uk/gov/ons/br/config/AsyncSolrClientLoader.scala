package uk.gov.ons.br.config


import com.typesafe.config.Config
import com.typesafe.config.ConfigException.BadValue
import io.ino.solrs.RetryPolicy.TryAvailableServers
import io.ino.solrs.future.ScalaFutureFactory.Implicit
import io.ino.solrs.{AsyncSolrClient, CloudSolrServers, RoundRobinLB}
import org.slf4j.Logger
import play.api.ConfigLoader
import uk.gov.ons.br.utils.{BaseUrl, Port}

import scala.concurrent.Future
import scala.util.control.NonFatal

class AsyncSolrClientLoader(logger: Logger) extends ConfigLoader[AsyncSolrClient[Future]] {

  private val Standalone = "standalone"

  override def load(rootConfig: Config, path: String): AsyncSolrClient[Future] = {
    val config = rootConfig.getConfig(path)
    val collection = config.getString("collection")
    if (config.hasPath(Standalone)) configureStandaloneClient(config.getConfig(Standalone), collection)
    else configureCloudClient(config.getConfig("cloud"), collection)
  }

  private def configureStandaloneClient(config: Config, collection: String): AsyncSolrClient[Future] = {
    val solrUrl = BaseUrl.asUrlString(baseUrlFrom(config)) + "/" + collection
    logger.info("Configuring Standalone Solr client with Url [{}]", solrUrl)
    AsyncSolrClient(solrUrl)
  }

  private def baseUrlFrom(config: Config): BaseUrl =
    BaseUrl(
      config.getString("protocol"),
      config.getString("host"),
      portFrom(config),
      Some(config.getString("prefix"))
    )

  private def portFrom(config: Config): Port = {
    val path = "port"
    val n = config.getInt(path)
    try Port(n)
    catch {
      case NonFatal(e) => throw new BadValue(path, e.getMessage)
    }
  }

  private def configureCloudClient(config: Config, collection: String): AsyncSolrClient[Future] = {
    import scala.collection.JavaConverters._
    val zkHosts = config.getStringList("zookeeperHosts").asScala.mkString(",")
    logger.info("Configuring Solr Cloud client with Zookeeper Hosts [{}]", zkHosts)
    val servers = new CloudSolrServers(zkHost = zkHosts, defaultCollection = Some(collection))
    AsyncSolrClient.Builder(RoundRobinLB(servers)).
      withRetryPolicy(TryAvailableServers).build
  }
}
