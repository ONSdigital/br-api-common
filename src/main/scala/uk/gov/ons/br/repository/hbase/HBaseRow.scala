package uk.gov.ons.br.repository.hbase

/*
 * Note that we originally modelled cells as a Map, having made the assumption that the fields belonging to a row
 * have unique names.  While this is true when reading data, the assumption does not hold for write operations,
 * where checkAndAct operations are modelled as a "row" containing two versions of the same cell - the before and
 * after values.
 */
case class HBaseRow(key: RowKey, cells: Seq[HBaseCell])

object HBaseRow {
  def asFields(row: HBaseRow): Map[String, String] =
    row.cells.map { cell =>
      cell.column -> cell.value
    }.toMap
}
