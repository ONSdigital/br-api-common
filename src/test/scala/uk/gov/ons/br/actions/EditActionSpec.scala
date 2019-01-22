package uk.gov.ons.br.actions

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.JsString
import play.api.mvc.Results.{BadRequest, NoContent}
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.ons.br.actions.EditAction.UserIdHeaderName
import uk.gov.ons.br.actions.EditActionSpec.{UserId, aPatchRequest}
import uk.gov.ons.br.models.patch.{Operation, Patch, TestOperation}
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class EditActionSpec extends UnitSpec with MockFactory with GuiceOneAppPerTest with ScalaFutures {

  private trait Fixture {
    private implicit val executionContext = ExecutionContext.Implicits.global

    val block = mockFunction[EditRequest[Patch], Future[Result]]
    val bodyParser = stub[BodyParser[Patch]]
    val underTest = new EditAction(bodyParser)
  }

  "An EditAction" - {
    "returns BadRequest when the X-User-Id header is not defined" in new Fixture {
      whenReady(underTest.async(block)(aPatchRequest)) { result =>
        result shouldBe BadRequest
      }
    }

    "invokes the controller block with an EditRequest when the X-User-Id header is defined" in new Fixture {
      val request = aPatchRequest.withHeaders(Headers(UserIdHeaderName -> UserId))
      block.expects(where[EditRequest[Patch]] { _.userId === UserId }).returning(Future.successful(NoContent))

      whenReady(underTest.async(block)(request)) { result =>
        result shouldBe NoContent
      }
    }
  }
}

object EditActionSpec {
  val UserId = "jdoe"
  val APatch = Seq(
    TestOperation("/links/ubrn", JsString("1234567890123456"))
  )

  def aPatchRequest: Request[Seq[Operation]] =
    FakeRequest("PATCH", "some-uri").withBody(APatch)
}