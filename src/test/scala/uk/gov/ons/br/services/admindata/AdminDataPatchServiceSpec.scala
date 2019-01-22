package uk.gov.ons.br.services.admindata


import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json._
import uk.gov.ons.br.models.Ubrn
import uk.gov.ons.br.models.patch.{Patch, ReplaceOperation, TestOperation}
import uk.gov.ons.br.repository.CommandRepository
import uk.gov.ons.br.repository.CommandRepository._
import uk.gov.ons.br.services.PatchService._
import uk.gov.ons.br.services.UnitRegistryService
import uk.gov.ons.br.services.UnitRegistryService.{UnitFound, UnitNotFound, UnitRegistryFailure}
import uk.gov.ons.br.services.admindata.AdminDataPatchServiceSpec._
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class AdminDataPatchServiceSpec extends UnitSpec with MockFactory with ScalaFutures {

  private trait Fixture {
    private implicit val executionContext = ExecutionContext.Implicits.global
    implicit val readsUbrn = mock[Reads[Ubrn]]
    val repository = mock[CommandRepository[FakeUnitRef, Ubrn]]
    val unitRegistryService = mock[UnitRegistryService[Ubrn]]
    val underTest = new AdminDataPatchService(repository, unitRegistryService)
  }

  "An Admin Data PatchService" - {
    "can apply a patch requesting a checked update of the Admin Unit's parent UBRN to that of a Legal Unit that exists in the register" in new Fixture {
      val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
      val updateParentCommand = UpdateParentLinkCommand(from = From, to = ToUbrn, editedBy = UserId)
      (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
      (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitFound))
      (repository.updateParentLink _).expects(AdminUnitRef, updateParentCommand).returning(Future.successful(EditApplied))

      whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
        result shouldBe PatchApplied
      }
    }

    "can signal that an attempt to apply a patch conflicted with another change" in new Fixture {
      val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
      val updateParentCommand = UpdateParentLinkCommand(from = From, to = ToUbrn, editedBy = UserId)
      (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
      (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitFound))
      (repository.updateParentLink _).expects(AdminUnitRef, updateParentCommand).returning(Future.successful(EditConflicted))

      whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
        result shouldBe PatchConflicted
      }
    }

    "can signal that the Admin Unit that is the subject of the patch could not be found" in new Fixture {
      val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
      val updateParentCommand = UpdateParentLinkCommand(from = From, to = ToUbrn, editedBy = UserId)
      (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
      (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitFound))
      (repository.updateParentLink _).expects(AdminUnitRef, updateParentCommand).returning(Future.successful(EditTargetNotFound))

      whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
        result shouldBe PatchTargetNotFound
      }
    }

    "can signal that an attempt to apply a patch failed (as a result of some error)" in new Fixture {
      val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
      val updateParentCommand = UpdateParentLinkCommand(from = From, to = ToUbrn, editedBy = UserId)
      val failureMessage = "some message"
      (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
      (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitFound))
      (repository.updateParentLink _).expects(AdminUnitRef, updateParentCommand).returning(Future.successful(EditFailed(failureMessage)))

      whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
        result shouldBe PatchFailed(failureMessage)
      }
    }

    "rejects a patch" - {
      "when the test path is not the parent link path" in new Fixture {
        val operationsWithInvalidTestPath = Seq(
          TestOperation("/links/legalUnit", JsString(From)),
          ReplaceOperation("/links/ubrn", ReplaceOperationJsValue)
        )

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, operationsWithInvalidTestPath))) { result =>
          result shouldBe PatchRejected("Unsupported test path [/links/legalUnit]")
        }
      }

      "when the replace path is not the parent link path" in new Fixture {
        val operationsWithInvalidReplacePath = Seq(
          TestOperation("/links/ubrn", JsString(From)),
          ReplaceOperation("/links/legalUnit", ReplaceOperationJsValue)
        )

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, operationsWithInvalidReplacePath))) { result =>
          result shouldBe PatchRejected("Unsupported replace path [/links/legalUnit]")
        }
      }

      "when both the test & replace paths are not the parent link path" in new Fixture {
        val operationsWithInvalidPath = Seq(
          TestOperation("/links/legalUnit", JsString(From)),
          ReplaceOperation("/links/legalUnit", ReplaceOperationJsValue)
        )

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, operationsWithInvalidPath))) { result =>
          result shouldBe PatchRejected("Unsupported test path [/links/legalUnit] and replace path [/links/legalUnit]")
        }
      }

      /*
       * Permitting a 'replace' operation by itself would not allow us to use a 'checkAndUpdate' operation,
       * and would run the risk of overwriting another user's concurrent changes.
       */
      "that does not contain both a 'test' and a 'replace' operation" in new Fixture {
        val replaceOnlyOperation = Seq(ReplaceOperation("/links/ubrn", ReplaceOperationJsValue))

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, replaceOnlyOperation))) { result =>
          result shouldBe PatchRejected("Unsupported sequence of operations")
        }
      }

      "containing a test value that is not a string" in new Fixture {
        val operationsWithInvalidFromValue = Seq(
          TestOperation("/links/ubrn", JsNumber(42)),
          ReplaceOperation("/links/ubrn", ReplaceOperationJsValue)
        )

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, operationsWithInvalidFromValue))) { result =>
          result shouldBe a [PatchRejected]
          result.asInstanceOf[PatchRejected].msg should startWith("test value must be a string")
        }
      }

      "containing a replacement value that is not a Ubrn" in new Fixture {
        val operationsWithInvalidToValue = Seq(
          TestOperation("/links/ubrn", JsString(From)),
          ReplaceOperation("/links/ubrn", ReplaceOperationJsValue)
        )
        (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsError("Invalid UBRN"))

        whenReady(underTest.applyPatchTo(AdminUnitRef, PatchDescriptor(editedBy = UserId, operationsWithInvalidToValue))) { result =>
          result shouldBe a [PatchRejected]
          result.asInstanceOf[PatchRejected].msg should startWith("replace value must have UBRN format")
        }
      }

      "requesting a checked update of the Admin Unit's parent UBRN to that of a Legal Unit that is not known to the register" in new Fixture {
        val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
        (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
        (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitNotFound))

        whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
          result shouldBe PatchRejected(s"A Legal Unit with the target UBRN [$ToUbrn] was not found")
        }
      }
    }

    "fails when an error is encountered while attempting to confirm that the requested parent Legal Unit is known to the register" in new Fixture {
      val patch = PatchDescriptor(editedBy = UserId, parentUpdateOperations(test = JsString(From), replace = ReplaceOperationJsValue))
      val failureMessage = "some message"
      (readsUbrn.reads _).expects(ReplaceOperationJsValue).returning(JsSuccess(ToUbrn))
      (unitRegistryService.isRegistered _).expects(ToUbrn).returning(Future.successful(UnitRegistryFailure(failureMessage)))

      whenReady(underTest.applyPatchTo(AdminUnitRef, patch)) { result =>
        result shouldBe PatchFailed(s"Failed confirming that target UBRN exists [$failureMessage]")
      }
    }
  }
}

private object AdminDataPatchServiceSpec {
  case class FakeUnitRef(value: String)

  val AdminUnitRef = FakeUnitRef("1234567890")
  val From = "1111111111111111"
  val ToUbrn = Ubrn("9999999999999999")
  val ReplaceOperationJsValue = JsString("6666666666666666")
  val UserId = "auser"

  def parentUpdateOperations(test: JsString, replace: JsString): Patch = Seq(
    TestOperation("/links/ubrn", test),
    ReplaceOperation("/links/ubrn", replace)
  )
}