package uk.gov.ons.br.utils

import play.api.libs.json.{ JsError, JsResult, JsSuccess }

import scala.util.Try

object JsResultSupport {
  def fromTry[A](tryA: Try[A]): JsResult[A] =
    tryA.fold(
      err => JsError(err.getMessage),
      a => JsSuccess(a))
}
