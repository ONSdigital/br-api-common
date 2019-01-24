package uk.gov.ons.br.repository.hbase.rest

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalToJson}
import play.api.http.Status.{BAD_REQUEST, UNAUTHORIZED}
import play.api.libs.json.{JsResult, JsSuccess, Json}
import uk.gov.ons.br.repository.CommandRepository.{EditApplied, EditConflicted, EditFailed, EditTargetNotFound}
import uk.gov.ons.br.repository.hbase.rest.HBaseRestRepository_Update_WiremockSpec.{CheckAndUpdateRequestBody, CheckAndUpdateAndSetAdditionalFieldRequestBody, OldCell, OtherCell, RowKey, UpdatedCell}
import uk.gov.ons.br.repository.hbase.{HBaseCell, HBaseColumn, HBaseRow, RowKey}
import uk.gov.ons.br.test.hbase.HBaseJsonBodyBuilder

class HBaseRestRepository_Update_WiremockSpec extends AbstractHBaseRestRepositoryWiremockSpec {

  // uses a different value to the Query Spec to allow concurrent spec execution
  override protected val HBasePort = 8076

  private object FixtureHelper {
    private val DummyJsonResponseStr = """{"some":"json"}"""
    private val DummyJsonResponse = Json.parse(DummyJsonResponseStr)

    def stubHBaseTargetEntityLookupWillFail(withRowKey: RowKey)(implicit fixtureParam: FixtureParam): Unit = {
      import fixtureParam._
      stubHBaseFor(getHBaseJson(config.namespace, config.tableName, withRowKey, auth).willReturn(aServiceUnavailableResponse()))
    }

    def stubHBaseTargetEntityWillBeNotFound(withRowKey: RowKey)(implicit fixtureParam: FixtureParam): Unit =
      stubHBaseTargetEntityRetrieves(withRowKey, JsSuccess(Seq.empty))

    def stubHBaseTargetEntityWillBeFound(withRowKey: RowKey)(implicit fixtureParam: FixtureParam): Unit = {
      val row = HBaseRow(withRowKey, Seq(HBaseCell(HBaseColumn.name("family")("qualifier"), "value")))
      stubHBaseTargetEntityRetrieves(withRowKey, JsSuccess(Seq(row)))
    }

    private def stubHBaseTargetEntityRetrieves(rowKey: RowKey, bodyParseResult: JsResult[Seq[HBaseRow]])(implicit fixtureParam: FixtureParam): Unit = {
      import fixtureParam._
      stubHBaseFor(getHBaseJson(config.namespace, config.tableName, rowKey, auth).willReturn(anOkResponse().withBody(DummyJsonResponseStr)))
      (readsRows.reads _).expects(DummyJsonResponse).returning(bodyParseResult)
      () // explicitly return unit to avoid warning about disregarded return value
    }
  }

  import FixtureHelper._

  "A HBase REST Repository" - {
    "successfully applies an update to a row that has not been modified by another user" - {
      "when the update sets a single field" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(anOkResponse())
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditApplied
        }
      }

      "when the update sets multiple fields" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateAndSetAdditionalFieldRequestBody)).
          willReturn(anOkResponse())
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell, Seq(OtherCell))(logger)) { result =>
          result shouldBe EditApplied
        }
      }
    }

    "prevents an update" - {
      "when the field has been modified by another user" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(aNotModifiedResponse())
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditConflicted
        }
      }
    }

    "rejects an update" - {
      "when the target entity does not exist" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeNotFound(withRowKey = RowKey)

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditTargetNotFound
        }
      }
    }

    "fails an update" - {
      /*
       * Test patienceConfig must exceed the fixedDelay for this to work...
       */
      "when the server takes longer to respond than the configured client-side timeout" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(anOkResponse().withFixedDelay((config.timeout + 100).toInt))
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe a [EditFailed]
        }
      }

      "when the configured user credentials are not accepted" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(aResponse().withStatus(UNAUTHORIZED))
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditFailed("Unauthorized (401)")
        }
      }

      "when the attempt to confirm that the target entity exists fails" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityLookupWillFail(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(anOkResponse())
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe a [EditFailed]
          result.asInstanceOf[EditFailed].msg should startWith("Failure encountered performing ifExists lookup")
        }
      }

      "when the update response is a client error" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(aResponse().withStatus(BAD_REQUEST))
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditFailed("Bad Request (400)")
        }
      }

      "when the update response is a server error" in { implicit fixture =>
        import fixture._
        stubHBaseTargetEntityWillBeFound(withRowKey = RowKey)
        stubHBaseFor(checkedPutHBaseJson(namespace = config.namespace, tableName = config.tableName, auth = auth, rowKey = RowKey).
          withRequestBody(equalToJson(CheckAndUpdateRequestBody)).
          willReturn(aServiceUnavailableResponse())
        )

        whenReady(repository.updateRow(RowKey, checkCell = OldCell, updateCell = UpdatedCell)(logger)) { result =>
          result shouldBe EditFailed("Service Unavailable (503)")
        }
      }
    }
  }
}

private object HBaseRestRepository_Update_WiremockSpec extends HBaseJsonBodyBuilder {
  private val RowKey = "rowKey"
  private val ColumnFamily = "family"
  private val ColumnQualifier = "qualifier"
  private val ColumnName = HBaseColumn.name(ColumnFamily)(ColumnQualifier)
  private val OldCell = HBaseCell(ColumnName, "oldValue")
  private val UpdatedCell = HBaseCell(ColumnName, "updatedValue")
  private val OtherColumn = HBaseColumn("otherColumnFamily", "otherColumnQualifier")
  private val OtherCell = HBaseCell(HBaseColumn.name(OtherColumn), "otherColumnValue")

  private val CheckAndUpdateRequestBody =
    aBodyWith(
      aRowWith(key = RowKey,
        aColumnWith(family = ColumnFamily, qualifier = ColumnQualifier, value = UpdatedCell.value, timestamp = None),
        aColumnWith(family = ColumnFamily, qualifier = ColumnQualifier, value = OldCell.value, timestamp = None)
      )
    )

  private val CheckAndUpdateAndSetAdditionalFieldRequestBody =
    aBodyWith(
      aRowWith(key = RowKey,
        aColumnWith(family = ColumnFamily, qualifier = ColumnQualifier, value = UpdatedCell.value, timestamp = None),
        aColumnWith(family = OtherColumn.family, qualifier = OtherColumn.qualifier, value = OtherCell.value, timestamp = None),
        aColumnWith(family = ColumnFamily, qualifier = ColumnQualifier, value = OldCell.value, timestamp = None)
      )
    )
}