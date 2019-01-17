package uk.gov.ons.br.filters

import akka.stream.Materializer
import org.slf4j.Logger
import play.api.http.Status
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.ons.br.filters.AccessLoggingFilter.{LogEntryWithResultFormat, logEntryWithResultArguments}

import scala.concurrent.{ExecutionContext, Future}

/*
 * This is based on the example at: https://www.playframework.com/documentation/2.6.x/ScalaLogging
 *
 * Note that we are injecting an slf4j logger as this is an interface that can be stubbed / mocked.
 * Scala Logging only offers a concrete class, and ScalaMock cannot mock the Play logger because the
 * message is a byName parameter.  Note that benchmarks have confirmed that using slf4j parameterized
 * messages is faster than Scala String interpolation.
 */
class AccessLoggingFilter(logger: Logger)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  def apply(next: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val resultFuture = next(request)
    resultFuture.onComplete {
      _.fold(logRequestWithFailure(request, _), logRequestWithResult(request, _))
    }

    resultFuture
  }

  /*
   * As this is an access log we simply attempt to summarise the characteristics of cause, but avoid logging
   * the full stack trace.
   */
  private def logRequestWithFailure(request: RequestHeader, cause: Throwable): Unit =
    logger.error("{} {} from {} status=FAIL {}-{}", Array(request.method, request.uri, request.remoteAddress,
      cause.getClass.getSimpleName, cause.getMessage): _*)

  private def logRequestWithResult(request: RequestHeader, result: Result): Unit = {
    val statusCode = result.header.status
    if (Status.isServerError(statusCode)) {
      logger.error(LogEntryWithResultFormat, logEntryWithResultArguments(request, statusCode): _*)
    } else {
      if (logger.isInfoEnabled) logger.info(LogEntryWithResultFormat, logEntryWithResultArguments(request, statusCode): _*)
    }
  }
}

private object AccessLoggingFilter {
  val LogEntryWithResultFormat = "{} {} from {} status={}"

  /*
   * Must be Array[AnyRef] in order to map to a Java Object... varargs argument on slf4j.
   * This implies that scala.Int (a value type) must be converted to java.lang.Integer (for which there is an implicit conversion).
   */
  def logEntryWithResultArguments(request: RequestHeader, statusCode: Int): Array[AnyRef] =
    Array(request.method, request.uri, request.remoteAddress, statusCode: java.lang.Integer)
}
