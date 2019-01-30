package uk.gov.ons.br.repository.solr


import org.apache.solr.client.solrj.response.QueryResponse

import scala.concurrent.Future

trait SolrClient {
  def searchFor(term: String): Future[QueryResponse]
}
