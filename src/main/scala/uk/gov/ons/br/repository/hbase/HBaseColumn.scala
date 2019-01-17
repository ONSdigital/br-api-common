package uk.gov.ons.br.repository.hbase

/**
  * A HBase column identifier comprises a column family together with a qualifier.
  *
  * @param family the column family
  * @param qualifier the column qualifier
  */
// @throws IllegalArgumentException if family is blank
case class HBaseColumn(family: String, qualifier: String) {
  require(family.trim.nonEmpty, "Column family cannot be blank")
}

object HBaseColumn {
  private val Delimiter = ":"

  def name(family: String)(qualifier: String): String =
    name(HBaseColumn(family, qualifier))

  def name(column: HBaseColumn): String =
    column.family + Delimiter + column.qualifier
}
