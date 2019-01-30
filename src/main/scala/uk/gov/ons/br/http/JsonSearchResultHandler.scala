package uk.gov.ons.br.http


import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.{GatewayTimeout, InternalServerError, Ok}
import uk.gov.ons.br.repository.SearchResult
import uk.gov.ons.br.{RepositoryError, ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

class JsonSearchResultHandler[U](implicit writes: Writes[Seq[U]]) extends SearchResultHandler[U] {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def apply(searchResult: SearchResult[U]): Result = {
    val result = searchResult.fold(resultOnFailure, resultOnSuccess)
    searchResult.left.foreach(err => logger.error("Encountered search repository error [{}]", err))
    if (logger.isDebugEnabled) logger.debug("Translated searchResult of [{}] to HTTP status [{}]", searchResult, result.header.status)
    result
  }

  private def resultOnFailure(repositoryError: RepositoryError): Result =
    repositoryError match {
      case _: TimeoutRepositoryError => GatewayTimeout
      case _: ResultRepositoryError => InternalServerError
      case _: ServerRepositoryError => InternalServerError
    }

  private def resultOnSuccess(matches: Seq[U]): Result =
    Ok(Json.toJson(matches))
}
