package uk.gov.ons.br.repository

import uk.gov.ons.br.repository.CommandRepository.{OptimisticEditResult, UpdateParentLinkCommand}

import scala.concurrent.Future

trait CommandRepository[R, P] {
  def updateParentLink(unitRef: R, cmd: UpdateParentLinkCommand[P]): Future[OptimisticEditResult]
}

object CommandRepository {
  /*
   * As the 'from' value may be invalid / malformed, we cannot mandate that it has the parent reference type (P).
   */
  case class UpdateParentLinkCommand[P](from: String, to: P, editedBy: String)

  sealed trait OptimisticEditResult
  object EditApplied extends OptimisticEditResult
  object EditConflicted extends OptimisticEditResult
  object EditTargetNotFound extends OptimisticEditResult
  case class EditFailed(msg: String) extends OptimisticEditResult
}