package uk.gov.ons.br.parsers


import akka.util.ByteString
import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsValue
import play.api.libs.streams.Accumulator
import play.api.mvc.Results.{BadRequest, UnsupportedMediaType}
import play.api.mvc._
import uk.gov.ons.br.models.patch.Patch
import uk.gov.ons.br.parsers.JsonPatchBodyParser.JsonPatchMediaType

import scala.concurrent.ExecutionContext

/*
 * Note that the Json BodyParser injected into an instance of this class must be tolerant of the Content-Type
 * header value, as our target Content-Type is patch specific (application/json-patch+json).
 */
class JsonPatchBodyParser @Inject() (jsonBodyParser: BodyParser[JsValue])(implicit ec: ExecutionContext) extends BodyParser[Patch] {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  type JsonPatchBodyParserResult = Accumulator[ByteString, Either[Result, Patch]]

  /*
   * We use the underlying 'tolerantJson' parser because it will accept a Content-Type header that is not strictly json
   * (application/json or text/json), whereas the 'json' parser will not.
   */
  override def apply(rh: RequestHeader): JsonPatchBodyParserResult =
    rh.contentType.filter(_ == JsonPatchMediaType).fold[JsonPatchBodyParserResult](Accumulator.done(Left(UnsupportedMediaType))) { _ =>
      jsonBodyParser(rh).map { resultOrJsValue =>
        resultOrJsValue.flatMap(jsonToBadRequestOrPatch)
      }
    }

  private def jsonToBadRequestOrPatch(jsValue: JsValue): Either[Result, Patch] = {
    val validationErrorOrPatch = jsValue.validate[Patch].asEither
    validationErrorOrPatch.left.foreach { errors =>
      logger.error("Json document does not conform to Json Patch specification.  Input=[{}], errors=[{}].", jsValue, errors: Any)
    }
    validationErrorOrPatch.left.map(_ => BadRequest)
  }
}

object JsonPatchBodyParser {
  val JsonPatchMediaType = "application/json-patch+json"
}