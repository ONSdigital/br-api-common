package uk.gov.ons.br.repository.hbase

import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.{Clock, LocalDateTime}

import javax.inject.Inject
import org.slf4j.Logger
import uk.gov.ons.br.repository.CommandRepository
import uk.gov.ons.br.repository.CommandRepository.{OptimisticEditResult, UpdateParentLinkCommand}

import scala.concurrent.Future

class HBaseCommandRepository[R, P] @Inject() (hBase: HBaseRepository,
                                              makeRowKey: R => RowKey,
                                              makeParentValue: P => String,
                                              parentLinkColumn: HBaseColumn,
                                              makeEditHistoryId: () => HBaseColumn,
                                              clock: Clock)
                                             (implicit logger: Logger) extends CommandRepository[R, P] {
  private lazy val parentLinkColumnName = HBaseColumn.name(parentLinkColumn)
  private val parentLink: String => HBaseCell = HBaseCell(parentLinkColumnName, _)

  override def updateParentLink(unitRef: R, cmd: UpdateParentLinkCommand[P]): Future[OptimisticEditResult] = {
    if (logger.isDebugEnabled) logger.debug("Requesting [{}] is applied to unit with reference [{}]", cmd: Any, unitRef: Any)
    val toValue = makeParentValue(cmd.to)
    hBase.updateRow(
      rowKey = makeRowKey(unitRef),
      checkCell = parentLink(cmd.from),
      updateCell = parentLink(toValue),
      otherUpdateCells = Seq(makeEditHistoryCell(before = cmd.from, after = toValue, editedBy = cmd.editedBy))
    )
  }

  private def makeEditHistoryCell(before: String, after: String, editedBy: String): HBaseCell = {
    val editedAt = LocalDateTime.now(clock).format(ISO_LOCAL_DATE_TIME)
    val editHistoryValue = Seq(editedBy, editedAt, before, after).mkString("~")
    val editHistoryId = makeEditHistoryId()
    HBaseCell(HBaseColumn.name(editHistoryId), editHistoryValue)
  }
}
