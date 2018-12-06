package uk.gov.ons.br.repository.hbase.rest

import javax.inject.Inject
import org.slf4j.Logger
import play.api.http.HeaderNames.ACCEPT
import play.api.http.MimeTypes.JSON
import play.api.http.Status.{NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Reads}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.ons.br.repository.QueryResult
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepository.{bodyAsJson, describeStatus, withTranslationOfFailureToError}
import uk.gov.ons.br.repository.hbase.{HBaseRepository, HBaseRow, RowKey}
import uk.gov.ons.br.utils.BaseUrl
import uk.gov.ons.br.{RepositoryError, ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.util.Try
import scala.util.control.NonFatal

case class HBaseRestRepositoryConfig(baseUrl: BaseUrl,
                                     namespace: String,
                                     tableName: String,
                                     username: String,
                                     password: String,
                                     timeout: Long)

/*
 * The public finder methods exposed by this class should be implemented in such a way as to always
 * return a successful Future, materialising any failure in the result value (on the left).
 * This simplifies client interaction.
 *
 * Note that the client provides an slf4j logger, as this is an interface that can be stubbed / mocked.
 * Scala Logging only offers a concrete class, and we do not really want to depend on Play logging in the
 * repository layer.  We must therefore be aware of the cost of building log messages for a log level that
 * is not enabled.
 */
class HBaseRestRepository @Inject() (config: HBaseRestRepositoryConfig,
                                     wsClient: WSClient,
                                     readsRows: Reads[Seq[HBaseRow]])
                                    (implicit ec: ExecutionContext) extends HBaseRepository {

  override def findRow(rowKey: RowKey)(implicit logger: Logger): Future[QueryResult[HBaseRow]] =
    queryByRowKey(rowKey).map(verifyAtMostOneRow)

  private def queryByRowKey(rowKey: RowKey)(implicit logger: Logger): Future[Either[RepositoryError, Seq[HBaseRow]]] = {
    val url = urlForRow(rowKey)
    if (logger.isDebugEnabled) logger.debug("Requesting [{}] from HBase REST.", url)
    baseRequest(url).withHttpHeaders(ACCEPT -> JSON).get().map {
      (fromResponseToErrorOrJson _).andThen(convertToErrorOrRows)
    }.recover(withTranslationOfFailureToError)
  }

  private val urlForRow: RowKey => String =
    HBaseRestUrl.forRow(withBase = config.baseUrl, namespace = config.namespace, table = config.tableName, _)

  private def baseRequest(url: String): WSRequest =
    wsClient.
      url(url).
      withAuth(config.username, config.password, scheme = BASIC).
      withRequestTimeout(config.timeout.milliseconds)

  /*
   * Note that official environments running Cloudera will receive an OK result containing an "empty row" on Not Found.
   * Developers using HBase directly in a local environment will more than likely receive a 404.
   */
  private def fromResponseToErrorOrJson(response: WSResponse)(implicit logger: Logger): QueryResult[JsValue] = {
    if (logger.isDebugEnabled) logger.debug("HBase response has status [{}]", describeStatus(response))
    response.status match {
      case OK => bodyAsJson(response)
      case NOT_FOUND => Right(None)
      case UNAUTHORIZED => Left(ServerRepositoryError(describeStatus(response) + " - check HBase REST configuration"))
      case _ => Left(ServerRepositoryError(describeStatus(response)))
    }
  }

  private def convertToErrorOrRows(errorOrJson: QueryResult[JsValue])
                                  (implicit logger: Logger): Either[RepositoryError, Seq[HBaseRow]] =
    errorOrJson.flatMap { optJson =>
      if (logger.isDebugEnabled) logger.debug("HBase REST response JSON is [{}]", optJson)
      optJson.fold[Either[RepositoryError, Seq[HBaseRow]]](Right(Seq.empty)) { json =>
        parseJson(json)
      }
    }

  private def parseJson(json: JsValue)(implicit logger: Logger): Either[RepositoryError, Seq[HBaseRow]] = {
    val eitherErrorsOrRows = readsRows.reads(json).asEither
    if (logger.isDebugEnabled) logger.debug("HBase REST parsed response is [{}]", eitherErrorsOrRows)
    eitherErrorsOrRows.left.map(errors => ServerRepositoryError(s"Unable to parse HBase REST json response [$errors]."))
  }

  private def verifyAtMostOneRow(errorOrRows: Either[RepositoryError, Seq[HBaseRow]]): QueryResult[HBaseRow] =
    errorOrRows.flatMap {
      // order is important here - tail is not supported by an empty Seq
      case rows if rows.isEmpty => Right(None)
      case rows if rows.tail.isEmpty => Right(rows.headOption)
      case rows => Left(ResultRepositoryError(s"At most one result was expected but found [${rows.size}]"))
    }
}

private object HBaseRestRepository {
  private def bodyAsJson(response: WSResponse): QueryResult[JsValue] =
    Try(response.json).fold(
      err => Left(ServerRepositoryError(s"Unable to create JsValue from HBase response [${err.getMessage}]")),
      json => Right(json)).map(Some(_))

  private def describeStatus(response: WSResponse): String =
    s"${response.statusText} (${response.status})"

  private def withTranslationOfFailureToError[B]: PartialFunction[Throwable, Either[RepositoryError, B]] = {
    case timeout: TimeoutException => Left(TimeoutRepositoryError(timeout.getMessage))
    case NonFatal(t) => Left(ServerRepositoryError(t.getMessage))
  }
}