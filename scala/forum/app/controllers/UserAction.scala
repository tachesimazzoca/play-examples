package controllers

import models.AccountService
import models.UserLoginSession
import play.api.mvc._

import scala.concurrent.Future

object UserAction extends ActionBuilder[UserRequest] with ActionTransformer[Request, UserRequest] {

  val SESSION_KEY_USER_LOGIN = "user"

  val accountService = new AccountService
  val userLoginSession = new UserLoginSession

  override protected def transform[A](request: Request[A]): Future[UserRequest[A]] =
    Future.successful {
      val account = for { 
        key <- request.session.get(SESSION_KEY_USER_LOGIN)
        userLogin <- userLoginSession.find(key)
        a <- accountService.findById(userLogin.id)
      } yield a
      println(account)
      new UserRequest(account, request)
    }
}
