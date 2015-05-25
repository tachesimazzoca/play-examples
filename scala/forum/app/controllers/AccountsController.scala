package controllers

import components.form.AccountsEntryForm
import components.mail.{SignupMailer, SignupMail}
import models.{Signup, SignupService}
import play.api.mvc._

import scala.util.{Failure, Success}

object AccountsController extends Controller {

  val accountsEntryForm = AccountsEntryForm.defaultForm
  val signupService = new SignupService

  def entry = Action {
    Ok(views.html.accounts.entry(accountsEntryForm))
  }

  def postEntry = Action { implicit request =>
      accountsEntryForm.bindFromRequest.fold(
        form => BadRequest(views.html.accounts.entry(form)),
        data => {
          val sessionKey = signupService.create(Signup(data.email))
          val param = SignupMail(sessionKey)
          SignupMailer.send(data.email, param) match {
            case Success(x) => Ok(param.toString)
            case Failure(e) => BadRequest(e.getMessage)
          }
        }
      )
    }

  def activate(code: String) = TODO
}
