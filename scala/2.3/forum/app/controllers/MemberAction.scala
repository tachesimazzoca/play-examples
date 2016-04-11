package controllers

import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future

object MemberAction extends ActionFilter[UserRequest] {
  def filter[A](input: UserRequest[A]) = Future.successful {
    if (input.account.isDefined)
      None
    else
      Some(Redirect(routes.AccountsController.login()))
  }
}
