package controllers.action

import play.api.mvc._

class UserRequest[A](
  val sessionId: String,
  request: Request[A]
) extends WrappedRequest[A](request)
