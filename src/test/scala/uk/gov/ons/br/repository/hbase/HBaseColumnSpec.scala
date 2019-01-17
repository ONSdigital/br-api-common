package uk.gov.ons.br.repository.hbase

import uk.gov.ons.br.test.UnitSpec

class HBaseColumnSpec extends UnitSpec {

  "A fully qualified column name" - {
    "is comprised of" - {
      "a column family and a column family qualifier" in {
        HBaseColumn.name(HBaseColumn("family", "qualifier")) shouldBe "family:qualifier"
      }

      "where the column family may not be" - {
        "empty" in {
          an [IllegalArgumentException] should be thrownBy HBaseColumn("", "qualifier")
        }

        "blank" in {
          an [IllegalArgumentException] should be thrownBy HBaseColumn("  ", "qualifier")
        }
      }

      "where the column family qualifier may be empty" in {
        HBaseColumn.name(HBaseColumn("family", "")) shouldBe "family:"
      }
    }
  }

  "A partially applied column name consisting of only the family" - {
    "can be used to conveniently build multiple qualified column names" in {
      val aColumnNameQualifiedWith = HBaseColumn.name("family") _

      aColumnNameQualifiedWith("qualifier1") shouldBe "family:qualifier1"
      aColumnNameQualifiedWith("qualifier2") shouldBe "family:qualifier2"
    }
  }
}
