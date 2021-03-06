package uk.gov.ons.br.repository.hbase.rest


import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import play.api.http.Status.{BAD_REQUEST, UNAUTHORIZED}
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.ons.br.repository.hbase.HBaseColumn.name
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepository_Query_WiremockSpec._
import uk.gov.ons.br.repository.hbase.{HBaseCell, HBaseColumn, HBaseRow}
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

class HBaseRestRepository_Query_WiremockSpec extends AbstractHBaseRestRepositoryWiremockSpec {

  override protected val HBasePort = 8075

  "A HBase REST Repository" - {
    "when attempting to retrieve a specific row by the rowKey" - {

      "can process a valid success response" - {
        "containing a single row" in { fixture =>
          val targetRow = HBaseRow(key = RowKey, cells = Seq(HBaseCell(ColumnName, ColumnValue)))
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody(DummyJsonResponseStr)
          ))
          (fixture.readsRows.reads _).expects(Json.parse(DummyJsonResponseStr)).returning(
            JsSuccess(Seq(targetRow))
          )

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.right.value shouldBe Some(targetRow)
          }
        }

        /*
         * This is the NOT FOUND case when running against Cloudera, which returns an "empty row" (rather than a 404).
         */
        "containing no rows" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody(DummyJsonResponseStr)
          ))
          (fixture.readsRows.reads _).expects(Json.parse(DummyJsonResponseStr)).returning(
            JsSuccess(Seq.empty)
          )

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.right.value shouldBe None
          }
        }
      }

      /*
       * Currently only applicable for developers testing directly against a recent version of HBase.
       * This contrasts with the behaviour of Cloudera, which instead returns OK with a representation of an "empty row".
       */
      "can process a NOT FOUND response" in { fixture =>
        stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
          aNotFoundResponse()
        ))

        whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
          result.right.value shouldBe None
        }
      }

      "fails" - {
        "when multiple rows are returned" in { fixture =>
          val multipleRows = Seq(
            HBaseRow(key = "row1", cells = Seq(HBaseCell(ColumnName, ColumnValue))),
            HBaseRow(key = "row2", cells = Seq(HBaseCell(ColumnName, ColumnValue)))
          )
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody(DummyJsonResponseStr)
          ))
          (fixture.readsRows.reads _).expects(Json.parse(DummyJsonResponseStr)).returning(
            JsSuccess(multipleRows)
          )

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe ResultRepositoryError("At most one result was expected but found [2]")
          }
        }

        "when the configured user credentials are not accepted" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            aResponse().withStatus(UNAUTHORIZED)
          ))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe ServerRepositoryError("Unauthorized (401) - check HBase REST configuration")
          }
        }

        "when the response is a client error" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            aResponse().withStatus(BAD_REQUEST)
          ))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe ServerRepositoryError("Bad Request (400)")
          }
        }

        "when the response is a server error" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            aServiceUnavailableResponse()
          ))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe ServerRepositoryError("Service Unavailable (503)")
          }
        }

        "when an OK response is returned containing a non-JSON body" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody("this-is-not-json")
          ))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe a [ServerRepositoryError]
            result.left.value.asInstanceOf[ServerRepositoryError].msg should startWith("Unable to create JsValue from HBase response")
          }
        }

        "when an OK response contains a JSON body that cannot be parsed" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody(DummyJsonResponseStr)
          ))
          (fixture.readsRows.reads _).expects(Json.parse(DummyJsonResponseStr)).returning(JsError("parse failure"))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe a [ServerRepositoryError]
            result.left.value.asInstanceOf[ServerRepositoryError].msg should startWith("Unable to parse HBase REST json response")
          }
        }

        /*
         * Test patienceConfig must exceed the fixedDelay for this to work...
         */
        "when the server takes longer to respond than the configured client-side timeout" in { fixture =>
          stubHBaseFor(getHBaseJson(namespace = fixture.config.namespace, tableName = fixture.config.tableName, auth = fixture.auth, rowKey = RowKey).willReturn(
            anOkResponse().withBody(DummyJsonResponseStr).withFixedDelay((fixture.config.timeout + 100).toInt)
          ))

          whenReady(fixture.repository.findRow(RowKey)(fixture.logger)) { result =>
            result.left.value shouldBe a [TimeoutRepositoryError]
          }
        }
      }
    }
  }
}

private object HBaseRestRepository_Query_WiremockSpec {
  private val DummyJsonResponseStr = """{"some":"json"}"""
  private val RowKey = "rowKey"
  private val ColumnName = name(HBaseColumn("family", "qualifier"))
  private val ColumnValue = "columnValue"
}