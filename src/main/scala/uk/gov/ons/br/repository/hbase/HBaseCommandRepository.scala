package uk.gov.ons.br.repository.hbase

import javax.inject.Inject
import org.slf4j.Logger
import uk.gov.ons.br.repository.CommandRepository
import uk.gov.ons.br.repository.CommandRepository.OptimisticEditResult

import scala.concurrent.Future

class HBaseCommandRepository[R, P] @Inject() (hBase: HBaseRepository,
                                              makeRowKey: R => RowKey,
                                              makeParentValue: P => String,
                                              parentLinkColumn: HBaseColumn)
                                             (implicit logger: Logger) extends CommandRepository[R, P] {
  private lazy val parentLinkColumnName = HBaseColumn.name(parentLinkColumn)
  private val parentLink: String => HBaseCell = HBaseCell(parentLinkColumnName, _)

  override def updateParentLink(unitRef: R, from: String, to: P): Future[OptimisticEditResult] = {
    if (logger.isDebugEnabled) logger.debug("Requesting [{}] parent update from [{}] to [{}]", unitRef.toString, from, to.toString)
    hBase.updateRow(makeRowKey(unitRef), parentLink(from), parentLink(makeParentValue(to)))
  }
}
