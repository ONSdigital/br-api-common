package uk.gov.ons.br.repository.hbase.rest


import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Span}
import org.scalatest.{EitherValues, Matchers, Outcome}
import org.slf4j.Logger
import play.api.libs.json.Reads
import play.api.libs.ws.WSClient
import play.api.test.WsTestClient
import uk.gov.ons.br.repository.hbase.HBaseRow
import uk.gov.ons.br.test.hbase.{HBaseJsonRequestBuilder, WireMockHBase}
import uk.gov.ons.br.utils.{BaseUrl, Port}

import scala.concurrent.ExecutionContext

private[rest] abstract class AbstractHBaseRestRepositoryWiremockSpec extends org.scalatest.fixture.FreeSpec with WireMockHBase with
  HBaseJsonRequestBuilder with Matchers with EitherValues with MockFactory with ScalaFutures with PatienceConfiguration {

  // test timeout must exceed the configured HBaseRest timeout to properly test client-side timeout handling
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1500, Millis)), interval = scaled(Span(50, Millis)))

  protected case class FixtureParam(config: HBaseRestRepositoryConfig,
                                    auth: Authorization,
                                    repository: HBaseRestRepository,
                                    readsRows: Reads[Seq[HBaseRow]],
                                    logger: Logger)

  protected val HBasePort: Int

  override protected def withFixture(test: OneArgTest): Outcome =
    withWireMockHBase(HBasePort) { () =>
      WsTestClient.withClient { wsClient =>
        val fixture = makeFixtureParam(wsClient)
        withFixture(test.toNoArgTest(fixture))
      }(new play.api.http.Port(HBasePort))
    }

  protected def makeFixtureParam(wsClient: WSClient): FixtureParam = {
    val config = HBaseRestRepositoryConfig(
      BaseUrl(protocol = "http", host = "localhost", port = Port(HBasePort), prefix = None),
      namespace = "namespace", tableName = "table", username = "username", password = "password", timeout = 1000L)
    val auth = Authorization(config.username, config.password)
    val readsRows = mock[Reads[Seq[HBaseRow]]]
    val repository = new HBaseRestRepository(config, wsClient, readsRows)(ExecutionContext.global)
    val logger = stub[Logger]
    FixtureParam(config, auth, repository, readsRows, logger)
  }
}
