package uk.gov.ons.br.repository.hbase.rest


import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.utils.{BaseUrl, Port}

class HBaseRestUrlSpec extends UnitSpec {

  private trait Fixture {
    val baseUrl = BaseUrl(protocol = "http", host = "hostname", port = Port(1234), prefix = None)
  }

  "A HBase REST url can be built" - {
    "that identifies a specific table row by its rowKey" in new Fixture {
      val url = HBaseRestUrl.forRow(baseUrl, namespace = "namespace", table = "table", rowKey = "rowKey")

      url shouldBe "http://hostname:1234/namespace:table/rowKey"
    }

    "that specifies a checked put to a target row" in new Fixture {
      val url = HBaseRestUrl.forCheckedPut(baseUrl, namespace = "namespace", table = "table", rowKey = "rowKey")

      url shouldBe "http://hostname:1234/namespace:table/rowKey/?check=put"
    }
  }
}
