package uk.gov.ons.br.repository.hbase

import org.slf4j.Logger
import uk.gov.ons.br.repository.QueryResult

import scala.concurrent.Future

/*
 * The equivalent of this abstraction in SBR takes tableName as a function argument.
 * Enforcing a micro-service data ownership model, a repository is now locked to a dedicated namespace & table.
 */
trait HBaseRepository {
  def findRow(rowKey: RowKey)(implicit logger: Logger): Future[QueryResult[HBaseRow]]
}
