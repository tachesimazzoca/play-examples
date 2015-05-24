package controllers

import components.form.SignupForm
import components.mail.{SignupMail, SignupMailer}
import models.Signup
import models.SignupService
import play.api.data.Form
import play.api.mvc._

import scala.util.{Failure, Success}

object SignupController extends Controller {
  val signupForm: Form[SignupForm] = SignupForm()
  val signupService = new SignupService

  def entry = Action {
    Ok(views.html.signup.entry(signupForm))
  }

  def verify = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      form => BadRequest(views.html.signup.entry(form)),
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

  def complete(code: String) = TODO
}
