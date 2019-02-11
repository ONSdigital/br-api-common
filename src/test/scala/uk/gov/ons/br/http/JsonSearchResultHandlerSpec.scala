package uk.gov.ons.br.http


import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{JsArray, JsObject, JsString, Writes}
import play.api.mvc.Result
import play.api.test.Helpers._
import play.mvc.Http.MimeTypes.JSON
import uk.gov.ons.br.http.JsonSearchResultHandlerSpec.{FakeSearchUnit, SampleFakeSearchUnit, SampleFakeSearchUnitJson}
import uk.gov.ons.br.repository.SearchResult
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

import scala.concurrent.Future

class JsonSearchResultHandlerSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    implicit val writesFakeSearchUnit = mock[Writes[FakeSearchUnit]]
    private val underTest = JsonSearchResultHandler.apply[FakeSearchUnit]

    def handle(searchResult: SearchResult[FakeSearchUnit]): Future[Result] =
      Future.successful(underTest(searchResult))
  }

  "A Json SearchResult Handler" - {
    "returns OK with a JSON representation of the search result when non empty" in new Fixture {
      (writesFakeSearchUnit.writes _).expects(SampleFakeSearchUnit).returning(SampleFakeSearchUnitJson)

      val futResult = handle(Right(Seq(SampleFakeSearchUnit)))

      status(futResult) shouldBe OK
      contentType(futResult).value shouldBe JSON
      contentAsJson(futResult) shouldBe JsArray(Seq(SampleFakeSearchUnitJson))
    }

    "returns GATEWAY_TIMEOUT when the query result is a repository timeout error" in new Fixture {
      val futResult = handle(Left(TimeoutRepositoryError("Timeout")))

      status(futResult) shouldBe GATEWAY_TIMEOUT
    }

    "returns INTERNAL_SERVER_ERROR when the query result is a repository server error" in new Fixture {
      val futResult = handle(Left(ServerRepositoryError("Retrieval failed")))

      status(futResult) shouldBe INTERNAL_SERVER_ERROR
    }

    "returns INTERNAL_SERVER_ERROR when the query result contains invalid data" in new Fixture {
      val futResult = handle(Left(ResultRepositoryError("Bad data")))

      status(futResult) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}

private object JsonSearchResultHandlerSpec {
  type FakeSearchUnit = AnyVal
  val SampleFakeSearchUnit = 42L
  val SampleFakeSearchUnitJson = JsObject(Seq("name" -> JsString("value")))
}