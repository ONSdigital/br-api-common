package uk.gov.ons.br

sealed trait RepositoryError
// Repository did not return a result within an acceptable time period
case class TimeoutRepositoryError(msg: String) extends RepositoryError
// Repository returned a result that business logic considers invalid
case class ResultRepositoryError(msg: String) extends RepositoryError
// General case where repository signalled an error
case class ServerRepositoryError(msg: String) extends RepositoryError

package object repository {
  type QueryResult[U] = Either[RepositoryError, Option[U]]
  type SearchResult[U] = Either[RepositoryError, Seq[U]]
}
