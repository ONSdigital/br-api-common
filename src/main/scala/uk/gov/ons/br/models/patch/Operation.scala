package uk.gov.ons.br.models.patch


import play.api.libs.json.{ JsResult, JsValue, Reads }

sealed trait Operation {
  val path: String
}

final case class AddOperation(path: String, value: JsValue) extends Operation
final case class RemoveOperation(path: String) extends Operation
final case class ReplaceOperation(path: String, value: JsValue) extends Operation
final case class TestOperation(path: String, value: JsValue) extends Operation

object Operation {
  implicit val reads = new Reads[Operation] {
    override def reads(json: JsValue): JsResult[Operation] =
      json.validate[JsonPatchOperation].flatMap { patchOperation =>
        val op = patchOperation.op
        op.createOperation(patchOperation.path, json)
      }
  }
}
