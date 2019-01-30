package uk.gov.ons.br.actions


import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, BodyParser, Result}
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import uk.gov.ons.br.actions.DefaultSearchActionMakerSpec.{SampleSearchUnit, SearchTerm, SearchUnit}
import uk.gov.ons.br.repository.{SearchRepository, SearchResult}
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class DefaultSearchActionMakerSpec extends UnitSpec with MockFactory with GuiceOneAppPerTest with ScalaFutures {
  /*
   * This is unpleasant, but we need to extend the class under test in order to have access to the SearchResult type.
   * Our mock function and argument matcher need to be defined within the scope of the SearchResult type.
   *
   * Note that importantly we do not do anything that would prevent an instance of this class from acting exactly
   * the same as a DefaultSearchActionMaker.
   */
  private class FixtureSearchActionMaker(bodyParser: BodyParser[AnyContent], searchRepository: SearchRepository[SearchUnit])
                                        (implicit ec: ExecutionContext) extends DefaultSearchActionMaker[SearchUnit](bodyParser, searchRepository) {
    val block = mockFunction[SearchRequest[AnyContent], Future[Result]]

    def aRequest(withSearchResult: SearchResult[SearchUnit]): SearchRequest[AnyContent] => Boolean =
      request => request.searchResult == withSearchResult
  }

  private trait Fixture extends StubPlayBodyParsersFactory {
    private implicit val executionContext = ExecutionContext.Implicits.global
    private implicit val materializer = app.materializer

    val searchRepository = mock[SearchRepository[SearchUnit]]
    val underTest = new FixtureSearchActionMaker(stubPlayBodyParsers.default, searchRepository)
  }

  "A DefaultSearchActionMaker" - {
    "makes an action that performs a search and adds the search result to the request" in new Fixture {
      val searchResult = Right(Seq(SampleSearchUnit))
      (searchRepository.searchFor _).expects(SearchTerm).returning(Future.successful(searchResult))

      val blockResult = Ok("some-payload")
      underTest.block.expects(where(underTest.aRequest(withSearchResult = searchResult))).returning(Future.successful(blockResult))

      val actionFunction = underTest.forTerm(SearchTerm)

      whenReady(actionFunction.invokeBlock(FakeRequest("GET", "some-uri"), underTest.block)) { result =>
        result shouldBe blockResult
      }
    }
  }
}

private object DefaultSearchActionMakerSpec {
  case class SearchUnit(name: String, postcode: String)

  val SearchTerm = "some-term"
  val SampleSearchUnit = SearchUnit(name = "some-name", postcode = "some-postcode")
}