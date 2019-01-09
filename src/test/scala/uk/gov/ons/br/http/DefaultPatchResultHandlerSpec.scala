package uk.gov.ons.br.http


import play.api.mvc.Results.{Conflict, InternalServerError, NoContent, NotFound, UnprocessableEntity}
import uk.gov.ons.br.services.PatchService._
import uk.gov.ons.br.test.UnitSpec

class DefaultPatchResultHandlerSpec extends UnitSpec {

  "The DefaultPatchResultHandler" - {
    "returns NoContent when the application of a patch succeeds" in {
      DefaultPatchResultHandler(PatchApplied) shouldBe NoContent
    }

    "returns Conflict when the attempt to apply a patch conflicts with another edit" in {
      DefaultPatchResultHandler(PatchConflicted) shouldBe Conflict
    }

    "returns NotFound when the patch target cannot be found" in {
      DefaultPatchResultHandler(PatchTargetNotFound) shouldBe NotFound
    }

    "returns UnprocessableEntity when a patch is rejected" in {
      DefaultPatchResultHandler(PatchRejected("some reason")) shouldBe UnprocessableEntity
    }

    "returns InternalServerError when the application of a patch fails" in {
      DefaultPatchResultHandler(PatchFailed("some reason")) shouldBe InternalServerError
    }
  }
}
