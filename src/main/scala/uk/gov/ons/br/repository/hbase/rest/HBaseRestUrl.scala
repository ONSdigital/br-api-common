package uk.gov.ons.br.repository.hbase.rest


import uk.gov.ons.br.repository.hbase.RowKey
import uk.gov.ons.br.utils.BaseUrl

/*
 * Note that in SBR all HBase Rest queries specify the target column family, to overcome issues that were encountered
 * with prefix searches (i.e. those ending in a wildcard).
 * As we now only query HBase via an exact rowKey we can simplify the code by not specifying the target column
 * family up front.
 */
private[rest] object HBaseRestUrl {
  private val CheckedPutSuffix = "/?check=put"

  def forCheckedPut(withBase: BaseUrl, namespace: String, table: String, rowKey: RowKey): String =
    forRow(withBase, namespace, table, rowKey).concat(CheckedPutSuffix)

  def forCheckedPut(namespace: String, table: String, rowKey: RowKey): String =
    forRow(namespace, table, rowKey).concat(CheckedPutSuffix)

  def forRow(withBase: BaseUrl, namespace: String, table: String, rowKey: RowKey): String =
    s"${BaseUrl.asUrlString(withBase)}/${forRow(namespace, table, rowKey)}"

  def forRow(namespace: String, table: String, rowKey: RowKey): String =
    s"$namespace:$table/$rowKey"
}