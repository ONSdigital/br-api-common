package uk.gov.ons.br.repository

import scala.concurrent.Future

trait SearchRepository[U] {
  def searchFor(term: String): Future[SearchResult[U]]
}
