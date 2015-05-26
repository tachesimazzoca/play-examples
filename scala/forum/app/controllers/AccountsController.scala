package controllers

import components.form.AccountsEntryForm
import models.{Account, SignUp, SignUpMailer, SignUpSession}
import play.api.mvc._

object AccountsController extends Controller {

  val accountsEntryForm = AccountsEntryForm.defaultForm
  val signUpSession = new SignUpSession

  def entry = Action {
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = Action { implicit request =>
      accountsEntryForm.bindFromRequest.fold(
        form => BadRequest(views.html.accounts.entry(form)),
        data => {
          val Account.Password(hash, salt) = Account.hashPassword(data.password)
          val sessionKey = signUpSession.create(SignUp(data.email, hash, salt))
          val params = SignUpMailer.Params(sessionKey)
          val messageId = SignUpMailer.send(data.email, params)
          Ok(params.toString)
        }
      )
    }

  def activate(code: String) = TODO
}
