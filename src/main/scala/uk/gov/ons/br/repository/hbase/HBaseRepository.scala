package uk.gov.ons.br.repository.hbase

import org.slf4j.Logger
import uk.gov.ons.br.repository.CommandRepository.OptimisticEditResult
import uk.gov.ons.br.repository.QueryResult

import scala.concurrent.Future

/*
 * The equivalent of this abstraction in SBR takes tableName as a function argument.
 * Enforcing a micro-service data ownership model, a repository is now locked to a dedicated namespace & table.
 */
trait HBaseRepository {
  def findRow(rowKey: RowKey)(implicit logger: Logger): Future[QueryResult[HBaseRow]]

  // originally used a var-arg for otherUpdateCells but ScalaMock does not support this in a curried function
  def updateRow(rowKey: RowKey, checkCell: HBaseCell, updateCell: HBaseCell, otherUpdateCells: Seq[HBaseCell] = Seq.empty)
               (implicit logger: Logger): Future[OptimisticEditResult]
}
