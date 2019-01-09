package uk.gov.ons.br.services.admindata


import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsValue, Reads}
import uk.gov.ons.br.models.Ubrn
import uk.gov.ons.br.models.patch.{Patch, ReplaceOperation, TestOperation}
import uk.gov.ons.br.repository.CommandRepository
import uk.gov.ons.br.repository.CommandRepository._
import uk.gov.ons.br.services.PatchService._
import uk.gov.ons.br.services.UnitRegistryService.{UnitFound, UnitNotFound, UnitRegistryFailure}
import uk.gov.ons.br.services.admindata.AdminDataPatchService.{asPatchStatus, asRejectionOr}
import uk.gov.ons.br.services.{PatchService, UnitRegistryService}

import scala.concurrent.{ExecutionContext, Future}

/*
 * We assume that the Admin Unit is using the LinkToLegalUnit model and that this object is given the key "links".
 * This gives us the default ParentLinkPath - but this can be configured per client service at DI time.
 * Note that we treat this constructor argument as a constant within this class, and so capitalise it accordingly.
 * This makes it a 'stable identifier' that can then be used in patterns.
 */
class AdminDataPatchService[R] @Inject() (repository: CommandRepository[R, Ubrn],
                                          unitRegistryService: UnitRegistryService[Ubrn],
                                          ParentLinkPath: String = "/links/ubrn")
                                         (implicit ec: ExecutionContext, readsUbrn: Reads[Ubrn]) extends PatchService[R] {
  require(ParentLinkPath.startsWith("/"), "ParentLinkPath must start with '/'")
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def applyPatchTo(unitRef: R, patch: Patch): Future[PatchStatus] = {
    if (logger.isDebugEnabled) logger.debug("Applying patch to [{}] of specification [{}]", unitRef, patch)
    patch match {
      case TestOperation(ParentLinkPath, testValue) :: ReplaceOperation(ParentLinkPath, replaceValue) :: Nil =>
        (for {
          testStr <- asRejectionOr[String](testValue, rejectionMsg = "test value must be a string")
          replacementUbrn <- asRejectionOr[Ubrn](replaceValue, rejectionMsg = "replace value must have UBRN format")
        } yield (testStr, replacementUbrn)).fold[Future[PatchStatus]](
          Future.successful,
          { case (from, toUbrn) =>
            updateParentUbrn(unitRef, from, toUbrn)
          }
        )
      case TestOperation(unsupportedTestPath, _) :: ReplaceOperation(ParentLinkPath, _) :: Nil =>
        Future.successful(PatchRejected(s"Unsupported test path [$unsupportedTestPath]"))
      case TestOperation(ParentLinkPath, _) :: ReplaceOperation(unsupportedReplacePath, _) :: Nil =>
        Future.successful(PatchRejected(s"Unsupported replace path [$unsupportedReplacePath]"))
      case TestOperation(unsupportedTestPath, _) :: ReplaceOperation(unsupportedReplacePath, _) :: Nil =>
        Future.successful(PatchRejected(s"Unsupported test path [$unsupportedTestPath] and replace path [$unsupportedReplacePath]"))
      case _ =>
        Future.successful(PatchRejected("Unsupported sequence of operations"))
    }
  }

  /*
   * We check the target Legal Unit exists before updating a link to point to it.
   * This is a "best effort" attempt to maintain the consistency of the database.  As we have no way to perform an
   * atomic operation across tables, there is a risk (in future) that a concurrent process could delete the target
   * Legal Unit between the check and the update steps.  For now, we assume that units will not be outright deleted
   * and that this is sufficient.
   */
  private def updateParentUbrn(unitRef: R, from: String, toUbrn: Ubrn): Future[PatchStatus] =
    unitRegistryService.isRegistered(toUbrn).flatMap {
      case UnitFound => repository.updateParentLink(unitRef, from, toUbrn).map(toPatchStatus)
      case UnitNotFound => Future.successful(PatchRejected(s"A Legal Unit with the target UBRN [$toUbrn] was not found"))
      case UnitRegistryFailure(msg) => Future.successful(PatchFailed(s"Failed confirming that target UBRN exists [$msg]"))
    }

  private def toPatchStatus(editResult: OptimisticEditResult): PatchStatus = {
    val status = asPatchStatus(editResult)
    if (logger.isDebugEnabled) logger.debug("Translated editResult of [{}] to patch status [{}]", editResult, status: Any)
    status
  }
}

private object AdminDataPatchService {
  def asRejectionOr[A](value: JsValue, rejectionMsg: => String)(implicit reads: Reads[A]): Either[PatchRejected, A] = {
    val errorOrA = value.validate[A].asEither
    errorOrA.left.map(errors => PatchRejected(s"$rejectionMsg - [$errors]"))
  }

  def asPatchStatus(editResult: OptimisticEditResult): PatchStatus =
    editResult match {
      case EditApplied => PatchApplied
      case EditConflicted => PatchConflicted
      case EditTargetNotFound => PatchTargetNotFound
      case EditFailed(msg) => PatchFailed(msg)
    }
}