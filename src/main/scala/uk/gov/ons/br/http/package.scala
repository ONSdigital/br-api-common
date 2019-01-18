package uk.gov.ons.br


import play.api.mvc.Result
import uk.gov.ons.br.repository.QueryResult
import uk.gov.ons.br.services.PatchService.PatchStatus

package object http {
  type QueryResultHandler[U] = QueryResult[U] => Result
  type PatchResultHandler = PatchStatus => Result
}
