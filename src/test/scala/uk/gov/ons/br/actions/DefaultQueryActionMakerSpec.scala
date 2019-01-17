package uk.gov.ons.br.actions


import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, BodyParser, Result}
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import uk.gov.ons.br.actions.DefaultQueryActionMakerSpec.{FakeUnit, FakeUnitRef, SampleUnit, SampleUnitRef}
import uk.gov.ons.br.repository.{QueryRepository, QueryResult}
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class DefaultQueryActionMakerSpec extends UnitSpec with MockFactory with GuiceOneAppPerTest with ScalaFutures {
  /*
   * This is unpleasant, but we need to extend the class under test in order to have access to the QueryResult type.
   * Our mock function and argument matcher need to be defined within the scope of the QueryResult type.
   *
   * Note that importantly we do not do anything that would prevent an instance of this class from acting exactly
   * the same as a DefaultQueryActionMaker.
   */
  private class FixtureQueryActionMaker(bodyParser: BodyParser[AnyContent], queryRepository: QueryRepository[FakeUnitRef, FakeUnit])
                                       (implicit ec: ExecutionContext) extends DefaultQueryActionMaker[FakeUnitRef, FakeUnit](bodyParser, queryRepository) {
    val block = mockFunction[QueryRequest[AnyContent], Future[Result]]

    def aRequest(withQueryResult: QueryResult[FakeUnit]): QueryRequest[AnyContent] => Boolean =
      request => request.queryResult == withQueryResult
  }

  private trait Fixture extends StubPlayBodyParsersFactory {
    private implicit val executionContext = ExecutionContext.Implicits.global
    private implicit val materializer = app.materializer

    val queryRepository = mock[QueryRepository[FakeUnitRef, FakeUnit]]
    val underTest = new FixtureQueryActionMaker(stubPlayBodyParsers.default, queryRepository)
  }

  "A DefaultQueryActionMaker" - {
    "makes an action that performs a query and adds the query result to the request" in new Fixture {
      val queryResult = Right(Some(SampleUnit))
      (queryRepository.queryByUnitReference _).expects(SampleUnitRef).returning(Future.successful(queryResult))
      val blockResult = Ok("some-payload")
      underTest.block.expects(where(underTest.aRequest(withQueryResult = queryResult))).returning(Future.successful(blockResult))

      val actionFunction = underTest.byUnitReference(SampleUnitRef)

      whenReady(actionFunction.invokeBlock(FakeRequest("GET", "some-uri"), underTest.block)) { result =>
        result shouldBe blockResult
      }
    }
  }
}

private object DefaultQueryActionMakerSpec {
  case class FakeUnitRef(value: String)
  case class FakeUnit(ref: FakeUnitRef, name: String, postcode: String)

  val SampleUnitRef = FakeUnitRef("ABC123")
  val SampleUnit = FakeUnit(ref = SampleUnitRef, name = "some-name", postcode = "some-postcode")
}