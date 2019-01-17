package uk.gov.ons.br

import play.api.mvc.Result
import uk.gov.ons.br.repository.QueryResult

package object http {
  type QueryResultHandler[U] = QueryResult[U] => Result
}
