package uk.gov.ons.br.http


import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Ok

object JsonSearchResultHandler {
  implicit val searchSuccessHandler: SuccessHandler[Seq] = new SuccessHandler[Seq] {
    override def onSuccess[U](values: Seq[U])(implicit writes: Writes[U]): Result =
      Ok(Json.toJson(values))
  }

  def apply[U](implicit writes: Writes[U]): SearchResultHandler[U] =
    new JsonResultHandler[Seq].handle[U](_)
}
