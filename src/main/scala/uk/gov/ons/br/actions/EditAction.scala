package uk.gov.ons.br.actions

import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import uk.gov.ons.br.actions.EditAction.UserIdHeaderName
import uk.gov.ons.br.models.patch.Patch

import scala.concurrent.{ExecutionContext, Future}

/*
 * From the Play docs: "A controller in Play is nothing more than an object that generates Action values."
 * Actions are therefore the preferred approach to re-using behaviour across Controllers.
 *
 * Here we define an action that extracts the editedBy userId from a header, and makes it available from
 * a custom request object.  This custom request is passed to the controller block, meaning that the controller
 * should define a block of type EditRequest[Patch] => Result.
 * If the userId header is not defined, this action responds with BadRequest.
 */

class EditRequest[A](val userId: String, originalRequest: Request[A]) extends WrappedRequest[A](originalRequest)

class EditAction @Inject() (bodyParser: BodyParser[Patch])(implicit ec: ExecutionContext) extends ActionBuilder[EditRequest, Patch] with ActionRefiner[Request, EditRequest] {
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override protected def refine[A](request: Request[A]): Future[Either[Result, EditRequest[A]]] = {
    val optUserId = request.headers.get(UserIdHeaderName)
    val resultOrEditRequest = optUserId.fold[Either[Result, EditRequest[A]]] {
      logger.error(s"Mandatory header is missing [$UserIdHeaderName]")
      Left(BadRequest)
    } { userId =>
      Right(new EditRequest[A](userId, request))
    }

    Future.successful(resultOrEditRequest)
  }

  override protected def executionContext: ExecutionContext = ec
  override def parser: BodyParser[Patch] = bodyParser
}

object EditAction {
  val UserIdHeaderName = "X-User-Id"
}
