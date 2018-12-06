package uk.gov.ons.br.repository.hbase

import org.slf4j.Logger

trait HBaseRowMapper[U] {
  def fromRow(row: HBaseRow)(implicit logger: Logger): Option[U]
}
