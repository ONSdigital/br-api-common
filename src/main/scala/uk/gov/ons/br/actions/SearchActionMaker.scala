package uk.gov.ons.br.actions


import javax.inject.Inject
import play.api.mvc._
import uk.gov.ons.br.repository.{SearchRepository, SearchResult}

import scala.concurrent.{ExecutionContext, Future}

/*
 * From the Play docs: "A controller in Play is nothing more than an object that generates Action values."
 * Actions are therefore the preferred approach to re-using behaviour across Controllers.
 *
 * Here we define an action that encapsulates searching for a term across a repository.
 * The search result is added to the request, and this custom request is passed to the controller block.
 * The controller simply has to define a block of type: SearchRequest[AnyContent] => Result where the passed
 * SearchRequest instance already contains the search result.
 */

trait SearchActionMaker[U] {
  class SearchRequest[A](val searchResult: SearchResult[U], originalRequest: Request[A]) extends WrappedRequest[A](originalRequest)

  def forTerm(term: String): ActionBuilder[SearchRequest, AnyContent] with ActionTransformer[Request, SearchRequest]
}

class DefaultSearchActionMaker[U] @Inject() (bodyParser: BodyParser[AnyContent], searchRepository: SearchRepository[U])
                                            (implicit ec: ExecutionContext) extends SearchActionMaker[U] {
  override def forTerm(term: String): ActionBuilder[SearchRequest, AnyContent] with ActionTransformer[Request, SearchRequest] =
    new ActionBuilder[SearchRequest, AnyContent] with ActionTransformer[Request, SearchRequest]  {
      override protected def transform[A](request: Request[A]): Future[SearchRequest[A]] =
        searchRepository.searchFor(term).map(new SearchRequest(_, request))

      override protected def executionContext: ExecutionContext = ec
      override def parser: BodyParser[AnyContent] = bodyParser
    }
}