package uk.gov.ons.br.repository.hbase

import java.time.Month.JANUARY
import java.time.ZoneOffset.UTC
import java.time._

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import uk.gov.ons.br.models.Ubrn
import uk.gov.ons.br.repository.CommandRepository.{EditApplied, UpdateParentLinkCommand}
import uk.gov.ons.br.repository.hbase.HBaseColumn.name
import uk.gov.ons.br.repository.hbase.HBaseCommandRepositorySpec._
import uk.gov.ons.br.test.UnitSpec

import scala.concurrent.Future

class HBaseCommandRepositorySpec extends UnitSpec with MockFactory with ScalaFutures {

  private trait Fixture {
    val hBase = mock[HBaseRepository]
    val makeRowKey = mockFunction[FakeUnitRef, RowKey]
    val makeParentValue = mockFunction[Ubrn, String]
    val makeEditHistoryId = mockFunction[HBaseColumn]
    val clock = Clock.fixed(EditedAtInstant, UTC)
    private implicit val logger = stub[Logger]

    val underTest = new HBaseCommandRepository[FakeUnitRef, Ubrn](hBase, makeRowKey, makeParentValue, ParentLinkColumn, makeEditHistoryId, clock)
  }

  "A HBase Command Repository" - {
    "applies the target command to a HBase database when the update of a parent link is requested" in new Fixture {
      val updateParentCommand = UpdateParentLinkCommand(from = OldParentLinkValue, to = NewParentUbrn, editedBy = UserId)
      makeRowKey.expects(UnitRef).returning(RowKey)
      makeParentValue.expects(NewParentUbrn).returning(NewParentLinkValue)
      makeEditHistoryId.expects().returning(EditHistoryColumn)
      (hBase.updateRow(_: RowKey, _: HBaseCell, _: HBaseCell, _: Seq[HBaseCell])(_: Logger)).expects(
        RowKey,
        HBaseCell(ColumnName, OldParentLinkValue),
        HBaseCell(ColumnName, NewParentLinkValue),
        Seq(HBaseCell(name(EditHistoryColumn), s"$UserId~$EditedAt~$OldParentLinkValue~$NewParentLinkValue")),
        *
      ).returning(Future.successful(EditApplied))

      whenReady(underTest.updateParentLink(UnitRef, updateParentCommand)) { result =>
        result shouldBe EditApplied
      }
    }
  }
}

private object HBaseCommandRepositorySpec {
  case class FakeUnitRef(value: String)

  val UnitRef = FakeUnitRef("ABC123")
  val ParentLinkColumn = HBaseColumn(family = "data", qualifier = "parent")
  val ColumnName = name(ParentLinkColumn)
  val RowKey = "a-row-key"
  val OldParentLinkValue = "old-parent-link"
  val NewParentLinkValue = "new-parent-link"
  val NewParentUbrn = Ubrn("9999999999999999")
  val UserId = "auser"
  val EditHistoryColumn = HBaseColumn(family = "history", qualifier = "some-edit-id")
  val EditedAtInstant = LocalDateTime.of(2019, JANUARY, 30, 14, 15, 16, 123000000).toInstant(UTC)
  val EditedAt = "2019-01-30T14:15:16.123"
}