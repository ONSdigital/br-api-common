package uk.gov.ons.br.repository.hbase

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import uk.gov.ons.br.models.Ubrn
import uk.gov.ons.br.repository.CommandRepository.EditApplied
import uk.gov.ons.br.repository.hbase.HBaseCommandRepositorySpec._
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.Future

class HBaseCommandRepositorySpec extends UnitSpec with MockFactory with ScalaFutures {

  private trait Fixture {
    val hBase = mock[HBaseRepository]
    val makeRowKey = mockFunction[FakeUnitRef, RowKey]
    val makeParentValue = mockFunction[Ubrn, String]
    val parentLinkColumn = HBaseColumn(ColumnFamily, ParentLinkColumnQualifier)
    private implicit val logger = stub[Logger]
    val underTest = new HBaseCommandRepository[FakeUnitRef, Ubrn](hBase, makeRowKey, makeParentValue, parentLinkColumn)
  }

  "A HBase Command Repository" - {
    "applies the target command to a HBase database when the update of a parent link is requested" in new Fixture {
      val updatedParentUbrn = Ubrn("9999999999999999")
      makeRowKey.expects(UnitRef).returning(RowKey)
      makeParentValue.expects(updatedParentUbrn).returning(NewParentLinkValue)
      (hBase.updateRow(_: RowKey, _: HBaseCell, _: HBaseCell)(_: Logger)).
        expects(RowKey, HBaseCell(ColumnName, OldParentLinkValue), HBaseCell(ColumnName, NewParentLinkValue), *).
        returning(Future.successful(EditApplied))

      whenReady(underTest.updateParentLink(UnitRef, OldParentLinkValue, updatedParentUbrn)) { result =>
        result shouldBe EditApplied
      }
    }
  }
}

private object HBaseCommandRepositorySpec {
  case class FakeUnitRef(value: String)

  val UnitRef = FakeUnitRef("ABC123")
  val ColumnFamily = "family"
  val ParentLinkColumnQualifier = "parent"
  val ColumnName = HBaseColumn.name(ColumnFamily)(ParentLinkColumnQualifier)
  val RowKey = "a-row-key"
  val OldParentLinkValue = "old-parent-link"
  val NewParentLinkValue = "new-parent-link"
}