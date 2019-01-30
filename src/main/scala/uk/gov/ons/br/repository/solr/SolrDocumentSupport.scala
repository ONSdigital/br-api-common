package uk.gov.ons.br.repository.solr


import org.apache.solr.common.SolrDocument

import scala.collection.JavaConverters._

object SolrDocumentSupport {
  /*
   * Note that a SolrDocument may contain "multi-valued fields" i.e. multiple values for the same key.
   * We do not support such fields - and simply retain one field per key.
   *
   * Note that we do not use the getFieldValuesMap or getFieldValueMap methods on SolrDocument.  These
   * return Map implementations with many unsupported methods - resulting in a UnsupportedOperationException
   * when attempting to apply the standard JavaConverters.
   */
  def asFields(document: SolrDocument): Map[String, String] =
    document.entrySet().asScala.toSeq.map { e =>
      e.getKey -> e.getValue
    }.toMap.mapValues(_.toString)
}
