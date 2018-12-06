package uk.gov.ons.br.repository.hbase

import javax.inject.Inject
import org.slf4j.Logger
import uk.gov.ons.br.repository.{QueryRepository, QueryResult}
import uk.gov.ons.br.{RepositoryError, ResultRepositoryError}

import scala.concurrent.{ExecutionContext, Future}

/*
 * Adds a "business object" view of the underlying repository.
 * This shields client code from the underlying representation of a HBase request / response.
 *
 * Note that we are injecting an slf4j logger as this is an interface that can be stubbed / mocked.
 * Scala Logging only offers a concrete class, and we do not really want to depend on Play logging in the
 * repository layer.  We must therefore be aware of the cost of building log messages for a log level that
 * is not enabled.
 */
class HBaseQueryRepository[R, U] @Inject() (hBase: HBaseRepository,
                                            rowMapper: HBaseRowMapper[U],
                                            makeRowKey: R => RowKey)
                                           (implicit ec: ExecutionContext, logger: Logger) extends QueryRepository[R, U] {
  override def queryByUnitReference(unitRef: R): Future[QueryResult[U]] = {
    val rowKey = makeRowKey(unitRef)
    if (logger.isDebugEnabled) logger.debug("Translated unit reference [{}] to rowKey [{}].", unitRef, rowKey)
    queryByRowKey(rowKey)
  }

  private def queryByRowKey(rowKey: RowKey): Future[QueryResult[U]] =
    hBase.findRow(rowKey).map { errorOrPossibleRow =>
      if (logger.isDebugEnabled) logger.debug("Query result for rowKey [{}] is [{}].", rowKey: Any, errorOrPossibleRow: Any)
      errorOrPossibleRow.flatMap { optRow =>
        optRow.fold[QueryResult[U]](Right(None)) { row =>
          fromRow(row).map(Some(_))
        }
      }
    }

  private def fromRow(row: HBaseRow): Either[RepositoryError, U] = {
    val optUnit = rowMapper.fromRow(row)
    optUnit.fold(ifEmpty = logger.warn("Unable to construct a unit from the row [{}].", row)) { unit =>
      if (logger.isDebugEnabled) logger.debug("Constructed unit [{}] from the row [{}].", unit, row)
    }
    optUnit.toRight(ResultRepositoryError("Unable to construct a unit from the HBase row"))
  }
}
