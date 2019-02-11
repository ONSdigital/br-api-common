package uk.gov.ons.br.repository.solr


import org.apache.solr.client.solrj.SolrResponse
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import uk.gov.ons.br.repository.solr.SolrSearchRepositorySpec.{FakeUnit, Term, aQueryResponse, aSolrDocumentWithId}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.solr.SolrResponseBuilder
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError}

import scala.concurrent.{ExecutionContext, Future}

class SolrSearchRepositorySpec extends UnitSpec with MockFactory with ScalaFutures {

  private trait Fixture {
    private implicit val executionContext = ExecutionContext.Implicits.global
    private implicit val logger = stub[Logger]

    val solrClient = mock[SolrClient]
    val solrDocumentMapper = mock[SolrDocumentMapper[FakeUnit]]
    val underTest = new SolrSearchRepository[FakeUnit](solrClient, solrDocumentMapper)
  }

  "A Solr Search Repository" - {
    "may return no matches" in new Fixture {
      (solrClient.searchFor _).expects(Term).returning(
        Future.successful(aQueryResponse(withMatches = Seq.empty))
      )

      whenReady(underTest.searchFor(Term)) { result =>
        result.right.value shouldBe Seq.empty
      }
    }

    "may return multiple matches" in new Fixture {
      (solrClient.searchFor _).expects(Term).returning(
        Future.successful(aQueryResponse(withMatches = Seq(
          Map("id" -> "doc1"),
          Map("id" -> "doc2")
        )))
      )
      (solrDocumentMapper.fromDocument(_: SolrDocument)(_: Logger)).expects(where { aSolrDocumentWithId("doc1") }).returning(
        Some(FakeUnit("match1"))
      )
      (solrDocumentMapper.fromDocument(_: SolrDocument)(_: Logger)).expects(where { aSolrDocumentWithId("doc2") }).returning(
        Some(FakeUnit("match2"))
      )

      whenReady(underTest.searchFor(Term)) { result =>
        result.right.value shouldBe Seq(
          FakeUnit("match1"),
          FakeUnit("match2")
        )
      }
    }

    "skips invalid match documents when some conversions fail" in new Fixture {
      (solrClient.searchFor _).expects(Term).returning(
        Future.successful(aQueryResponse(withMatches = Seq(
          Map("id" -> "doc1"),
          Map("id" -> "doc2")
        )))
      )
      (solrDocumentMapper.fromDocument(_: SolrDocument)(_: Logger)).expects(where { aSolrDocumentWithId("doc1") }).returning(
        None
      )
      (solrDocumentMapper.fromDocument(_: SolrDocument)(_: Logger)).expects(where { aSolrDocumentWithId("doc2") }).returning(
        Some(FakeUnit("match2"))
      )

      whenReady(underTest.searchFor(Term)) { result =>
        result.right.value shouldBe Seq(
          FakeUnit("match2")
        )
      }
    }

    "indicates failure when all match document conversions fail" in new Fixture {
      (solrClient.searchFor _).expects(Term).returning(
        Future.successful(aQueryResponse(withMatches = Seq(
          Map("id" -> "doc1"),
          Map("id" -> "doc2")
        )))
      )
      (solrDocumentMapper.fromDocument(_: SolrDocument)(_: Logger)).expects(*, *).repeated(2).returning(
        None
      )

      whenReady(underTest.searchFor(Term)) { result =>
        result.left.value shouldBe a [ResultRepositoryError]
      }
    }

    "materialises an error into a failure" in new Fixture {
      (solrClient.searchFor _).expects(Term).returning(Future.failed(new Exception("search failure")))

      whenReady(underTest.searchFor(Term)) { result =>
        result.left.value shouldBe a [ServerRepositoryError]
      }
    }
  }
}

private object SolrSearchRepositorySpec {
  val Term = "some-term"

  case class FakeUnit(value: String)

  def aQueryResponse(withMatches: Seq[Map[String, AnyRef]]): QueryResponse =
    newQueryResponse(SolrResponseBuilder.toSolrResponseFor(query = Term)(withMatches))

  private def newQueryResponse(solrResponse: SolrResponse): QueryResponse = {
    val response = new QueryResponse()
    response.setResponse(solrResponse.getResponse)
    response
  }

  def aSolrDocumentWithId(id: String): (SolrDocument, Logger) => Boolean =
    (solrDoc: SolrDocument, _: Logger) => solrDoc.getFieldValue("id") == id
}