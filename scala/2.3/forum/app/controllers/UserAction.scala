package controllers

import models._
import play.api.mvc._

import scala.concurrent.Future

class UserAction(components: ComponentRegistry) extends ActionBuilder[UserRequest]
  with ActionTransformer[Request, UserRequest] {

  override protected def transform[A](request: Request[A]): Future[UserRequest[A]] =
    Future.successful {
      val account = for {
        key <- request.session.get(UserAction.SESSION_KEY_USER_LOGIN)
        userLogin <- components.userLoginSession.find(key)
        a <- components.accountService.findById(userLogin.id)
      } yield a
      println(account)
      new UserRequest(account, request)
    }
}

object UserAction {
  val SESSION_KEY_USER_LOGIN = "user"
}
