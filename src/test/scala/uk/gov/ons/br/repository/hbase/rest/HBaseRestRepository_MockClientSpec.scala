package uk.gov.ons.br.repository.hbase.rest


import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import play.api.libs.json.Reads
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import uk.gov.ons.br.ServerRepositoryError
import uk.gov.ons.br.repository.hbase.HBaseRow
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.utils.{BaseUrl, Port}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/*
 * This spec mocks the wsClient, which disregards the rule "don't mock types you don't own" (see "Growing
 * Object-Oriented Software, Guided by Tests" by Freeman & Pryce).  Prefer the sibling test that uses Wiremock
 * where possible.  This was introduced to simplify asserting that the client-side timeout is configured correctly,
 * as this is not observable via Wiremock.  It also allows us to assert that the configured host / port are used,
 * as the wsTestClient used by the acceptance test overrides these.
 */
class HBaseRestRepository_MockClientSpec extends UnitSpec with MockFactory with ScalaFutures with OneInstancePerTest {

  private trait Fixture {
    val Protocol = "http"
    val Host = "somehost"
    val PortNumber = 4321
    val ClientTimeout = 3321L
    val AwaitTime = 500.milliseconds
    val RowKey = "rowKey"
    val config = HBaseRestRepositoryConfig(
      BaseUrl(protocol = Protocol, host = Host, port = Port(PortNumber), prefix = Some("HBase")),
      namespace = "namespace", tableName = "table", username = "username", password = "password", timeout = ClientTimeout)

    implicit val logger = stub[Logger]
    val readsRows = stub[Reads[Seq[HBaseRow]]]
    val wsClient = stub[WSClient]
    val wsRequest = mock[WSRequest]
    val wsResponse = stub[WSResponse]
    val underTest = new HBaseRestRepository(config, wsClient, readsRows)(ExecutionContext.global)

    def expectRequestHeadersAndAuth(): Unit = {
      (wsRequest.withHttpHeaders _).expects(*).returning(wsRequest)
      (wsRequest.withAuth _).expects(*, *, *).returning(wsRequest)
      () // explicitly return unit to avoid warning about disregarded return value
    }
  }

  "A HBase REST repository" - {
    "when querying data" - {
      "specifies the configured client-side timeout when making a request" in new Fixture {
        (wsClient.url _).when(*).returns(wsRequest)
        expectRequestHeadersAndAuth()
        (wsRequest.withRequestTimeout _).expects(ClientTimeout.milliseconds).returning(wsRequest)
        (wsRequest.get _).expects().returning(Future.successful(wsResponse))

        Await.result(underTest.findRow(RowKey), AwaitTime)
      }

      "targets the specified host and port when making a request" in new Fixture {
        (wsClient.url _).when(where[String](_.startsWith(s"$Protocol://$Host:$PortNumber"))).returning(wsRequest)
        expectRequestHeadersAndAuth()
        (wsRequest.withRequestTimeout _).expects(*).returning(wsRequest)
        (wsRequest.get _).expects().returning(Future.successful(wsResponse))

        Await.result(underTest.findRow(RowKey), AwaitTime)
      }

      /*
       * Any connection failed / socket disconnected type issue will likely result in the WsRequest's
       * Future failing.  This tests the "catch-all" case, and that we can effectively recover the Future.
       */
      "materialises an error into a failure" in new Fixture {
        (wsClient.url _).when(*).returns(wsRequest)
        expectRequestHeadersAndAuth()
        (wsRequest.withRequestTimeout _).expects(*).returning(wsRequest)
        (wsRequest.get _).expects().returning(Future.failed(new Exception("Connection failed")))

        whenReady(underTest.findRow(RowKey)) { result =>
          result.left.value shouldBe ServerRepositoryError("Connection failed")
        }
      }
    }
  }
}
