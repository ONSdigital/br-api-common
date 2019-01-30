package uk.gov.ons.br.repository.solr.solrs


import io.ino.solrs.AsyncSolrClient
import javax.inject.Inject
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import uk.gov.ons.br.repository.solr.SolrClient

import scala.concurrent.Future

class SolrsClientWrapper @Inject() (delegate: AsyncSolrClient[Future]) extends SolrClient {
  override def searchFor(term: String): Future[QueryResponse] =
    delegate.query(new SolrQuery(term))
}
