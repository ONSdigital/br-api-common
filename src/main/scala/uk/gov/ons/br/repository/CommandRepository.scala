package uk.gov.ons.br.repository

import uk.gov.ons.br.repository.CommandRepository.OptimisticEditResult

import scala.concurrent.Future

trait CommandRepository[R, P] {
  /*
   * As the 'from' value may be invalid / malformed, we cannot mandate that it has the parent reference type (P).
   */
  def updateParentLink(unitRef: R, from: String, to: P): Future[OptimisticEditResult]
}

object CommandRepository {
  sealed trait OptimisticEditResult
  object EditApplied extends OptimisticEditResult
  object EditConflicted extends OptimisticEditResult
  object EditTargetNotFound extends OptimisticEditResult
  case class EditFailed(msg: String) extends OptimisticEditResult
}