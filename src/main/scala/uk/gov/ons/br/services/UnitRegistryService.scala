package uk.gov.ons.br.services


import uk.gov.ons.br.services.UnitRegistryService.UnitRegistryResult

import scala.concurrent.Future

trait UnitRegistryService[R] {
  def isRegistered(unitRef: R): Future[UnitRegistryResult]
}

object UnitRegistryService {
  sealed trait UnitRegistryResult
  object UnitFound extends UnitRegistryResult
  object UnitNotFound extends UnitRegistryResult
  case class UnitRegistryFailure(msg: String) extends UnitRegistryResult
}