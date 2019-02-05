package uk.gov.ons.br.repository.solr


import org.apache.solr.common.SolrDocument
import org.slf4j.Logger

trait SolrDocumentMapper[U] {
  def fromDocument(document: SolrDocument)(implicit logger: Logger): Option[U]
}
