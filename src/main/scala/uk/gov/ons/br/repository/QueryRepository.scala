package uk.gov.ons.br.repository

import scala.concurrent.Future

trait QueryRepository[R, U] {
  def queryByUnitReference(unitRef: R): Future[QueryResult[U]]
}
