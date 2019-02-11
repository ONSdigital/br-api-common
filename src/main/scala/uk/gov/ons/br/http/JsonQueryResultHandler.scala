package uk.gov.ons.br.http


import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.{NotFound, Ok}
import uk.gov.ons.br.repository.QueryResult

@deprecated(message = "Use the companion object to obtain a suitable QueryResultHandler implementation", since = "v1.4")
class JsonQueryResultHandler[U](implicit writes: Writes[U]) extends QueryResultHandler[U] {
  override def apply(queryResult: QueryResult[U]): Result =
    JsonQueryResultHandler.apply[U](writes)(queryResult)
}

object JsonQueryResultHandler {
  implicit val querySuccessHandler: SuccessHandler[Option] = new SuccessHandler[Option] {
    override def onSuccess[U](optValue: Option[U])(implicit writes: Writes[U]): Result =
      optValue.fold[Result](NotFound) { value =>
        Ok(Json.toJson(value))
      }
  }

  def apply[U](implicit writes: Writes[U]): QueryResultHandler[U] =
    new JsonResultHandler[Option].handle[U](_)
}
