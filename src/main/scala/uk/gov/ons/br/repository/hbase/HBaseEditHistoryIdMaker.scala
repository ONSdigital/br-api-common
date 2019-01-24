package uk.gov.ons.br.repository.hbase

import java.util.UUID

object HBaseEditHistoryIdMaker {
  def apply(columnFamily: String): () => HBaseColumn =
    () => HBaseColumn(family = columnFamily, qualifier = UUID.randomUUID().toString)
}
