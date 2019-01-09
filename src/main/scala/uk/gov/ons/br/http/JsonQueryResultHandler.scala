package uk.gov.ons.br.http

import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.{GatewayTimeout, InternalServerError, NotFound, Ok}
import uk.gov.ons.br.repository.QueryResult
import uk.gov.ons.br.{RepositoryError, ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

class JsonQueryResultHandler[U](implicit writes: Writes[U]) extends QueryResultHandler[U] {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def apply(queryResult: QueryResult[U]): Result = {
    val result = queryResult.fold(resultOnFailure, resultOnSuccess)
    queryResult.left.foreach(err => logger.error("Encountered repository error [{}]", err))
    if (logger.isDebugEnabled) logger.debug("Translated queryResult of [{}] to HTTP status [{}]", queryResult, result.header.status)
    result
  }

  private def resultOnFailure(repositoryError: RepositoryError): Result =
    repositoryError match {
      case _: TimeoutRepositoryError => GatewayTimeout
      case _: ResultRepositoryError => InternalServerError
      case _: ServerRepositoryError => InternalServerError
    }

  private def resultOnSuccess(optValue: Option[U]): Result =
    optValue.fold[Result](NotFound) { value =>
      Ok(Json.toJson(value))
    }
}
