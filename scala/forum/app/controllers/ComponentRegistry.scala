package controllers

import models._

trait ComponentRegistry {
  val accountService: AccountService
  val userLoginSession: UserLoginSession
  val signUpSession: SignUpSession
}
