package controllers.action

import models.Account
import play.api.mvc.WrappedRequest

class MemberRequest[A](
  val account: Account,
  val userRequest: UserRequest[A]
) extends WrappedRequest[A](userRequest)
