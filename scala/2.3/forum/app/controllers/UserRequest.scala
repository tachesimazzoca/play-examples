package controllers

import models.Account
import play.api.mvc._

class UserRequest[A](val account: Option[Account],
                     request: Request[A]) extends WrappedRequest[A](request)
