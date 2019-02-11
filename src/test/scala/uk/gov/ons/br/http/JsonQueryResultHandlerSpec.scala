package uk.gov.ons.br.http


import org.scalamock.scalatest.MockFactory
import play.api.http.MimeTypes.JSON
import play.api.libs.json.{JsObject, JsString, Writes}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.ons.br.http.JsonQueryResultHandlerSpec.{FakeUnit, SampleFakeUnit, SampleFakeUnitJson}
import uk.gov.ons.br.repository.QueryResult
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

import scala.concurrent.Future

class JsonQueryResultHandlerSpec extends UnitSpec with MockFactory {

  private trait Fixture {
    implicit val writesFakeUnit = mock[Writes[FakeUnit]]
    private val underTest = JsonQueryResultHandler.apply[FakeUnit]

    def handle(queryResult: QueryResult[FakeUnit]): Future[Result] =
      Future.successful(underTest(queryResult))
  }

  "A Json QueryResult Handler" - {
    "returns OK with a JSON representation of the query result when non empty" in new Fixture {
      (writesFakeUnit.writes _).expects(SampleFakeUnit).returning(SampleFakeUnitJson)

      val futResult = handle(Right(Some(SampleFakeUnit)))

      status(futResult) shouldBe OK
      contentType(futResult).value shouldBe JSON
      contentAsJson(futResult) shouldBe SampleFakeUnitJson
    }

    "returns NOT_FOUND when the query result is empty" in new Fixture {
      val futResult = handle(Right(None))

      status(futResult) shouldBe NOT_FOUND
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

private object JsonQueryResultHandlerSpec {
  type FakeUnit = AnyVal
  val SampleFakeUnit = 42L
  val SampleFakeUnitJson = JsObject(Seq("name" -> JsString("value")))
}