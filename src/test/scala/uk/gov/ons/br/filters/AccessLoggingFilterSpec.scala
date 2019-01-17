package uk.gov.ons.br.filters

import akka.stream.Materializer
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.slf4j.event.Level.{ERROR, INFO}
import play.api.http.HeaderNames.HOST
import play.api.mvc.Results.{InternalServerError, NotFound, Ok}
import play.api.mvc.{AnyContentAsEmpty, RequestHeader, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.ons.br.filters.AccessLoggingFilterSpec.{Method, RemoteAddress, Uri, fakeRequest}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.logging.{LogEntry, LoggingSpy}

import scala.concurrent.{ExecutionContext, Future}

/*
 * Note that this test utilises a 'LoggingSpy' rather than a mock Logger.
 * This allows our test to focus at a higher level of abstraction.  We can assert that a target log entry was
 * created, rather than a particular function was called with a particular format and arguments.
 * We have taken this approach because we want our implementation to be able to take advantage of the higher
 * performance string formatting of slf4j without the test having to be extremely specific about formats and
 * arguments (and therefore potentially brittle).
 */
class AccessLoggingFilterSpec extends UnitSpec with MockFactory with ScalaFutures with GuiceOneAppPerTest {

  private trait WithLoggingSpy {
    val loggingSpy: LoggingSpy
  }

  private trait InfoLoggingSpy extends WithLoggingSpy {
    override val loggingSpy: LoggingSpy = LoggingSpy.info("access@info")
  }

  private trait ErrorLoggingSpy extends WithLoggingSpy {
    override val loggingSpy: LoggingSpy = LoggingSpy.error("access@error")
  }

  private trait Fixture { this: WithLoggingSpy =>
    private implicit val materializer: Materializer = app.materializer
    private implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
    val request = fakeRequest
    val nextAction = mockFunction[RequestHeader, Future[Result]]

    lazy val underTest = new AccessLoggingFilter(loggingSpy)

    def awaitFilterLogging(request: RequestHeader): Future[Result] = {
      val futResult = underTest.apply(nextAction)(request)
      /*
       * We happen to know that nextAction has always been stubbed to return a fully resolved Future.
       * However, any logging side-effects from the filter may be executed asynchronously upon completion, and so
       * we need to give any logging a chance to complete before allowing the mock logger to self verify the logging
       * expectations.
       * If these tests appear to be flaky, this delay probably needs adjustment ...
       */
      Thread.sleep(500L)
      futResult
    }
  }

  "An AccessLoggingFilter" - {
    "when the appender log level is set to INFO" - {
      "passively logs the details of a successful request at INFO level" in new Fixture with InfoLoggingSpy {
        nextAction.expects(request).returning(Future.successful(Ok("some-payload")))

        awaitFilterLogging(request).futureValue shouldBe Ok("some-payload")

        loggingSpy.log should contain theSameElementsAs Seq(
          LogEntry(INFO, msg = s"$Method $Uri from $RemoteAddress status=200", t = None)
        )
      }

      "passively logs the details of a server error at ERROR level" in new Fixture with InfoLoggingSpy {
        nextAction.expects(request).returning(Future.successful(InternalServerError("server error")))

        awaitFilterLogging(request).futureValue shouldBe InternalServerError("server error")

        loggingSpy.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"$Method $Uri from $RemoteAddress status=500", t = None)
        )
      }

      "passively logs the details of other requests (including client errors) at INFO level" in new Fixture with InfoLoggingSpy {
        nextAction.expects(request).returning(Future.successful(NotFound))

        awaitFilterLogging(request).futureValue shouldBe NotFound

        loggingSpy.log should contain theSameElementsAs Seq(
          LogEntry(INFO, msg = s"$Method $Uri from $RemoteAddress status=404", t = None)
        )
      }

      // just in case the Future fails and we did not successfully recover ...
      "passively logs the details of any request without a HTTP response at ERROR level" in new Fixture with InfoLoggingSpy {
        val cause = new Exception("request failure")
        nextAction.expects(request).returning(Future.failed(cause))

        awaitFilterLogging(request).failed.futureValue shouldBe cause

        loggingSpy.log should contain theSameElementsAs Seq(
          LogEntry(ERROR, msg = s"$Method $Uri from $RemoteAddress status=FAIL Exception-request failure", t = None)
        )
      }
    }

    "when the appender log level is set to ERROR" - {
      "does not log details of successful requests" in new Fixture with ErrorLoggingSpy {
        nextAction.expects(request).returning(Future.successful(Ok("some-payload")))

        awaitFilterLogging(request).futureValue shouldBe Ok("some-payload")

        loggingSpy.log should be (empty)
      }

      "does not log details of non-server error requests" in new Fixture with ErrorLoggingSpy {
        nextAction.expects(request).returning(Future.successful(NotFound))

        awaitFilterLogging(request).futureValue shouldBe NotFound

        loggingSpy.log should be (empty)
      }
    }
  }
}

private object AccessLoggingFilterSpec {
  private val Method = "GET"
  private val Uri = "some-uri"
  private val RemoteAddress = "168.0.0.1"
  private val Headers = FakeHeaders(Seq(HOST -> "localhost"))

  def fakeRequest: RequestHeader =
    FakeRequest(Method, Uri, Headers, body = AnyContentAsEmpty, RemoteAddress)
}