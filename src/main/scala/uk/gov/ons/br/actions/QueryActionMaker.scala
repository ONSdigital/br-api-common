package uk.gov.ons.br.actions

import javax.inject.Inject
import play.api.mvc._
import uk.gov.ons.br.repository.{QueryRepository, QueryResult}

import scala.concurrent.{ExecutionContext, Future}

/*
 * From the Play docs: "A controller in Play is nothing more than an object that generates Action values."
 * Actions are therefore the preferred approach to re-using behaviour across Controllers.
 *
 * Here we define an action that encapsulates retrieving a unit from a repository by the unit reference.
 * The query result is added to the request, and this custom request is passed to the controller block.
 * The controller simply has to define a block of type: QueryRequest[AnyContent] => Result where the passed
 * QueryRequest instance already contains the query result.
 */

trait QueryActionMaker[R, U] {
  class QueryRequest[A](val queryResult: QueryResult[U], originalRequest: Request[A]) extends WrappedRequest[A](originalRequest)

  def byUnitReference(unitRef: R): ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest]
}

class DefaultQueryActionMaker[R, U] @Inject()(bodyParser: BodyParser[AnyContent], queryRepository: QueryRepository[R, U])
                                             (implicit ec: ExecutionContext) extends QueryActionMaker[R, U] {
  override def byUnitReference(unitRef: R): ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest] =
    new ActionBuilder[QueryRequest, AnyContent] with ActionTransformer[Request, QueryRequest] {
      override protected def transform[A](request: Request[A]): Future[QueryRequest[A]] =
        queryRepository.queryByUnitReference(unitRef).map(new QueryRequest(_, request))

      override protected def executionContext: ExecutionContext = ec
      override def parser: BodyParser[AnyContent] = bodyParser
    }
}