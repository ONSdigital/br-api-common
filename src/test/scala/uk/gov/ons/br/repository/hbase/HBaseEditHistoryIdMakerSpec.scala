package uk.gov.ons.br.repository.hbase

import java.util.UUID

import org.scalatest.matchers.{BeMatcher, MatchResult}
import uk.gov.ons.br.repository.hbase.HBaseEditHistoryIdMakerSpec.{ColumnFamily, IsUUIDMatcher}
import uk.gov.ons.br.test.UnitSpec

import scala.util.Try

class HBaseEditHistoryIdMakerSpec extends UnitSpec {

  private trait Fixture {
    val aUUID = new IsUUIDMatcher
    val makeId = HBaseEditHistoryIdMaker(ColumnFamily)
  }

  "A HBase EditHistory Id Maker" - {
    "makes a column with the expected column family" in new Fixture {
      makeId().family shouldBe ColumnFamily
    }

    "makes a column with a unique qualifier" in new Fixture {
      val id1 = makeId()
      val id2 = makeId()

      id1.qualifier should not be id2.qualifier
      id1.qualifier should be (aUUID)
      id2.qualifier should be (aUUID)
    }
  }
}

object HBaseEditHistoryIdMakerSpec {
  val ColumnFamily = "f"

  class IsUUIDMatcher extends BeMatcher[String] {
    override def apply(left: String): MatchResult = {
      val tryUUID = Try(UUID.fromString(left))
      MatchResult(
        matches = tryUUID.isSuccess,
        rawFailureMessage = s"$left was not a UUID",
        rawNegatedFailureMessage = s"$left was a UUID"
      )
    }
  }
}