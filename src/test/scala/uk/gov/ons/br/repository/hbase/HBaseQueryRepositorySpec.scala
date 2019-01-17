package uk.gov.ons.br.repository.hbase

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import uk.gov.ons.br.repository.hbase.HBaseQueryRepositorySpec._
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.{ResultRepositoryError, ServerRepositoryError, TimeoutRepositoryError}

import scala.concurrent.{ExecutionContext, Future}

class HBaseQueryRepositorySpec extends UnitSpec with MockFactory with ScalaFutures {

  private trait Fixture {
    private implicit val executionContext = ExecutionContext.Implicits.global
    private implicit val logger = stub[Logger]

    val hBase = mock[HBaseRepository]
    val rowMapper = mock[HBaseRowMapper[FakeUnit]]
    val rowKeyMaker = mockFunction[FakeUnitRef, RowKey]
    val underTest = new HBaseQueryRepository[FakeUnitRef, FakeUnit](hBase, rowMapper, rowKeyMaker)
  }

  "A HBase Query Repository" - {
    "can retrieve a unit by the unit reference" - {
      "returning the target unit when it exists and is valid" in new Fixture {
        rowKeyMaker.expects(SampleUnitRef).returning(SampleRowKey)
        (hBase.findRow(_: RowKey)(_: Logger)).expects(SampleRowKey, *).returning(Future.successful(Right(Some(SampleRow))))
        (rowMapper.fromRow(_: HBaseRow)(_: Logger)).expects(SampleRow, *).returning(Some(SampleUnit))

        whenReady(underTest.queryByUnitReference(SampleUnitRef)) { result =>
          result.right.value shouldBe Some(SampleUnit)
        }
      }

      "returning nothing when the target unit does not exist" in new Fixture {
        rowKeyMaker.expects(SampleUnitRef).returning(SampleRowKey)
        (hBase.findRow(_: RowKey)(_: Logger)).expects(SampleRowKey, *).returning(Future.successful(Right(None)))

        whenReady(underTest.queryByUnitReference(SampleUnitRef)) { result =>
          result.right.value shouldBe None
        }
      }

      "returning an error when a valid unit cannot be constructed from the found data" in new Fixture {
        rowKeyMaker.expects(SampleUnitRef).returning(SampleRowKey)
        (hBase.findRow(_: RowKey)(_: Logger)).expects(SampleRowKey, *).returning(Future.successful(Right(Some(SampleRow))))
        (rowMapper.fromRow(_: HBaseRow)(_: Logger)).expects(SampleRow, *).returning(None)

        whenReady(underTest.queryByUnitReference(SampleUnitRef)) { result =>
          result.left.value shouldBe ResultRepositoryError("Unable to construct a unit from the HBase row")
        }
      }

      "returning an error when a failure is encountered querying HBase" in new Fixture {
        rowKeyMaker.expects(SampleUnitRef).returning(SampleRowKey)
        (hBase.findRow(_: RowKey)(_: Logger)).expects(SampleRowKey, *).returning(Future.successful(Left(ServerRepositoryError("findRow error"))))

        whenReady(underTest.queryByUnitReference(SampleUnitRef)) { result =>
          result.left.value shouldBe ServerRepositoryError("findRow error")
        }
      }

      "returning an error when a HBase query fails to respond within an acceptable time period" in new Fixture {
        rowKeyMaker.expects(SampleUnitRef).returning(SampleRowKey)
        (hBase.findRow(_: RowKey)(_: Logger)).expects(SampleRowKey, *).returning(Future.successful(Left(TimeoutRepositoryError("Timeout"))))

        whenReady(underTest.queryByUnitReference(SampleUnitRef)) { result =>
          result.left.value shouldBe TimeoutRepositoryError("Timeout")
        }
      }
    }
  }
}

private object HBaseQueryRepositorySpec {
  case class FakeUnitRef(value: String)
  case class FakeUnit(ref: FakeUnitRef, name: String, postcode: String)

  val SampleUnitRef = FakeUnitRef("ABC123")
  val SampleUnit = FakeUnit(ref = SampleUnitRef, name = "some-name", postcode = "some-postcode")
  val SampleRowKey = "sample-row-key"
  val SampleRow = HBaseRow(key = SampleRowKey, cells = Seq(
    HBaseCell("family:column1", "value1"),
    HBaseCell("family:column2", "value2"))
  )
}