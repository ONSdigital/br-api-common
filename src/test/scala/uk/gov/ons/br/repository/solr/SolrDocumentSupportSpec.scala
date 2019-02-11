package uk.gov.ons.br.repository.solr


import java.util

import org.apache.solr.common.SolrDocument
import uk.gov.ons.br.repository.solr.SolrDocumentSupport.asFields
import uk.gov.ons.br.test.UnitSpec

import scala.collection.JavaConverters._
import scala.collection.mutable

class SolrDocumentSupportSpec extends UnitSpec {

  "A SolrDocument" - {
    "can be converted to a Map of fields" - {
      "when the document is empty" in {
        val solrDocument = new SolrDocument(new util.HashMap[String, AnyRef]())

        asFields(solrDocument) shouldBe Map.empty
      }

      "when the document consists of a single string value" in {
        val solrDocument = new SolrDocument(mutable.Map("key" -> ("value": AnyRef)).asJava)

        asFields(solrDocument) shouldBe Map("key" -> "value")
      }

      "when the document consists of a single integer value" in {
        val solrDocument = new SolrDocument(mutable.Map("key" -> (Int.box(42): AnyRef)).asJava)

        asFields(solrDocument) shouldBe Map("key" -> "42")
      }

      "when the document contains multiple fields" in {
        val solrDocument = new SolrDocument(mutable.Map(
          "str" -> ("strvalue": AnyRef),
          "num" -> Int.box(42)).asJava
        )

        asFields(solrDocument) shouldBe Map("str" -> "strvalue", "num" -> "42")
      }
    }
  }
}
