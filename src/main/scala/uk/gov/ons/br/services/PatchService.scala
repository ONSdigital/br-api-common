package uk.gov.ons.br.services


import uk.gov.ons.br.models.patch.Patch
import uk.gov.ons.br.services.PatchService.PatchStatus

import scala.concurrent.Future

trait PatchService[R] {
  def applyPatchTo(unitRef: R, patch: Patch): Future[PatchStatus]
}

object PatchService {
  sealed trait PatchStatus
  case object PatchApplied extends PatchStatus
  case object PatchConflicted extends PatchStatus
  case object PatchTargetNotFound extends PatchStatus
  case class PatchRejected(msg: String) extends PatchStatus
  case class PatchFailed(msg: String) extends PatchStatus
}