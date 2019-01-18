package uk.gov.ons.br.http


import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Result
import play.api.mvc.Results.{Conflict, InternalServerError, NoContent, NotFound, UnprocessableEntity}
import uk.gov.ons.br.services.PatchService._

object DefaultPatchResultHandler extends PatchResultHandler {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def apply(patchStatus: PatchStatus): Result = {
    val result = toResult(patchStatus)
    logError(patchStatus)
    if (logger.isDebugEnabled) logger.debug("Translated patchStatus of [{}] to HTTP status [{}]", patchStatus, result.header.status)
    result
  }

  private def toResult(patchStatus: PatchStatus): Result =
    patchStatus match {
      case PatchApplied => NoContent
      case PatchConflicted => Conflict
      case PatchTargetNotFound => NotFound
      case PatchRejected(_) => UnprocessableEntity
      case PatchFailed(_) => InternalServerError
    }

  private def logError(patchStatus: PatchStatus): Unit =
    patchStatus match {
      case PatchRejected(msg) => logger.error("PatchRejected [{}]", msg)
      case PatchFailed(msg) => logger.error("PatchFailed [{}]", msg)
      case _ => ()
    }
}
