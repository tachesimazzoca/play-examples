package controllers.action

import components.storage.Storage
import javax.inject.{Inject, Named}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserAction @Inject() (
  val parser: BodyParsers.Default,
  @Named("sessionIdKey") sessionIdKey: String,
  @Named("sessionStorage") sessionStorage: Storage
)(implicit val executionContext: ExecutionContext) extends ActionBuilder[UserRequest, AnyContent] {

  override def invokeBlock[A](
    request: Request[A],
    block: (UserRequest[A]) => Future[Result]
  ): Future[Result] = {

    val userRequest = request.cookies.get(sessionIdKey).map { cookie =>
      val sessionId = sessionStorage.touch(cookie.value)
      new UserRequest(sessionId, request)
    }.getOrElse {
      new UserRequest(sessionStorage.create(), request)
    }

    block(userRequest).map { result =>
      result.withCookies(Cookie(sessionIdKey, userRequest.sessionId))
    }
  }
}
