package uk.gov.ons.br.controllers

import com.github.ghik.silencer.silent
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

/*
 * This controller does nothing but respond with Bad Request (400)
 *
 * @silent unused - although the arguments to any badRequest method are unused, they are required as the Play router
 *                  dictates that all path parameters are in fact passed through to the controller.
 */
@silent
@SuppressWarnings(Array("UnusedMethodParameter"))
@Singleton
class BadRequestController @Inject() (protected val controllerComponents: ControllerComponents) extends BaseController {
  def badRequest(arg: String): Action[AnyContent] = Action {
    BadRequest
  }
}
