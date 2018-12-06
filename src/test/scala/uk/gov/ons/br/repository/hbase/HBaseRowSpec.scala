package uk.gov.ons.br.repository.hbase

import uk.gov.ons.br.repository.hbase.HBaseColumn.name
import uk.gov.ons.br.repository.hbase.HBaseRowSpec._
import uk.gov.ons.br.test.UnitSpec

class HBaseRowSpec extends UnitSpec {
  "A HBaseRow" - {
    "can represent its cells as a mapping of column name to field value" in {
      HBaseRow.asFields(SampleRow) shouldBe Map(
        ColumnName1 -> ColumnValue1,
        ColumnName2 -> ColumnValue2
      )
    }
  }
}

private object HBaseRowSpec {
  val ColumnName1 = name(HBaseColumn(family = "cf1", qualifier = "cq1"))
  val ColumnName2 = name(HBaseColumn(family = "cf2", qualifier = "cq2"))
  val ColumnValue1 = "value1"
  val ColumnValue2 = "value2"

  val SampleRow = HBaseRow(key = "unused", cells = Seq(
    HBaseCell(column = ColumnName1, value = ColumnValue1),
    HBaseCell(column = ColumnName2, value = ColumnValue2)
  ))
}