package uk.gov.ons.br.http


import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.Writes
import play.api.mvc.Result
import play.api.mvc.Results.{GatewayTimeout, InternalServerError}
import uk.gov.ons.br.http.JsonResultHandler.handleResultOnFailure
import uk.gov.ons.br.{RepositoryError, ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

import scala.language.higherKinds

trait SuccessHandler[F[_]] {
  def onSuccess[U](successValue: F[U])(implicit writes: Writes[U]): Result
}

class JsonResultHandler[F[_]] {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def handle[U](repositoryResult: Either[RepositoryError, F[U]])
               (implicit handleResult: SuccessHandler[F], writes: Writes[U]): Result = {
    val result = repositoryResult.fold(handleResultOnFailure, handleResult.onSuccess[U])
    repositoryResult.left.foreach(err => logger.error("Encountered repository error [{}]", err))
    if (logger.isDebugEnabled)
      logger.debug("Translated repository result of [{}] to HTTP status [{}]", repositoryResult, result.header.status)
    result
  }
}

private object JsonResultHandler {
  private def handleResultOnFailure(repositoryError: RepositoryError): Result =
    repositoryError match {
      case _: TimeoutRepositoryError => GatewayTimeout
      case _: ResultRepositoryError => InternalServerError
      case _: ServerRepositoryError => InternalServerError
    }
}