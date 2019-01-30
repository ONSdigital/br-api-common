package uk.gov.ons.br.repository.solr.solrs


import io.ino.solrs.{AsyncSolrClient, CloudSolrServers, RoundRobinLB}
import uk.gov.ons.br.config.{SolrClientConfig, SolrCloudClientConfig, StandaloneSolrClientConfig}
import io.ino.solrs.RetryPolicy.TryAvailableServers
import io.ino.solrs.future.ScalaFutureFactory.Implicit

import scala.concurrent.Future

object AsyncSolrClientMaker {
  def asyncSolrClientFor(solrClientConfig: SolrClientConfig): AsyncSolrClient[Future] =
    solrClientConfig match {
      case StandaloneSolrClientConfig(baseUrl) =>
        AsyncSolrClient(baseUrl)
      case SolrCloudClientConfig(zookeeperHosts, collection) =>
        AsyncSolrClient.Builder(RoundRobinLB(new CloudSolrServers(zookeeperHosts, defaultCollection = Some(collection)))).
          withRetryPolicy(TryAvailableServers).build
    }
}
