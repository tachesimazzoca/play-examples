package controllers

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._

import scala.concurrent.Future

object GuestAction extends ActionBuilder[Request] {
  override def invokeBlock[A](request: Request[A],
                              block: (Request[A]) => Future[Result]): Future[Result] = {
    block(request).map { result =>
      result.withSession(request.session - UserAction.SESSION_KEY_USER_LOGIN)
    }
  }
}
