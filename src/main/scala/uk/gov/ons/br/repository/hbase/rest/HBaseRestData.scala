package uk.gov.ons.br.repository.hbase.rest

import java.nio.charset.StandardCharsets
import java.util.Base64

import com.github.ghik.silencer.silent
import play.api.libs.json._
import uk.gov.ons.br.repository.hbase.{HBaseCell, HBaseRow}

/*
 * HBase only stores bytes, and so all data sent to / read from HBase is Base64 encoded.
 * We shield the client from this, and provide a format that seamlessly decodes / encodes as required.
 */
object HBaseRestData {
  /*
   * These types model the underlying HBase representation.
   * As we are relying on automatic format derivation, the field names MUST match those used by HBase EXACTLY.
   */
  private case class EncodedRows(Row: Seq[EncodedRow])
  private case class EncodedRow(key: String, Cell: Seq[EncodedColumn])
  private case class EncodedColumn(column: String, `$`: String)

  /*
   * @silent unused - compiler warns that column & row formats are unused - but they are resolved implicitly
   */
  @silent private implicit val encodedColumnFormat: Format[EncodedColumn] = Json.format[EncodedColumn]
  @silent private implicit val encodedRowFormat: Format[EncodedRow] = Json.format[EncodedRow]
  private implicit val encodedRowsFormat: Format[EncodedRows] = Json.format[EncodedRows]

  private val Charset = StandardCharsets.UTF_8

  private object Decoder extends (EncodedRow => HBaseRow) {
    def apply(encodedRow: EncodedRow): HBaseRow =
      HBaseRow(key = decode(encodedRow.key), cells = encodedRow.Cell.map(decodeColumn))

    private def decodeColumn(encodedColumn: EncodedColumn): HBaseCell =
      HBaseCell(column = decode(encodedColumn.column), value = decode(encodedColumn.`$`))

    private def decode(value: String): String = {
      val bytes = Base64.getDecoder.decode(value)
      new String(bytes, Charset)
    }
  }

  private object Encoder extends (HBaseRow => EncodedRow) {
    def apply(row: HBaseRow): EncodedRow =
      EncodedRow(key = encode(row.key), Cell = row.cells.map(encodeColumn))

    private def encodeColumn(col: HBaseCell): EncodedColumn =
      EncodedColumn(column = encode(col.column), `$` = encode(col.value))

    private def encode(value: String): String = {
      val bytes = value.getBytes(Charset)
      Base64.getEncoder.encodeToString(bytes)
    }
  }

  implicit val format: Format[Seq[HBaseRow]] = new Format[Seq[HBaseRow]] {
    override def reads(json: JsValue): JsResult[Seq[HBaseRow]] =
      json.validate[EncodedRows].map {
        _.Row.map(Decoder)
      }

    override def writes(rows: Seq[HBaseRow]): JsValue = {
      val encodedRows = EncodedRows(rows.map(Encoder))
      Json.toJson(encodedRows)
    }
  }
}
