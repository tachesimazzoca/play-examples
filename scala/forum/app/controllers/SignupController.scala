package controllers

import play.api._
import play.api.mvc._

import play.api.data.Form
import play.api.Play.current

import scala.util.{Success, Failure}

import components.form.SignupForm
import components.mailer.{SignupMailer, SignupMail}
import components.mailer.settings._

import models.SignupSession

object SignupController extends Controller {
  val signupForm: Form[SignupForm] = SignupForm()

  def entry = Action {
    Ok(views.html.signup.entry(signupForm))
  }

  def verify = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      form => BadRequest(views.html.signup.entry(form)),
      data => {
        val sessionKey = SignupSession.create(SignupSession(data.email))
        val signupMail = SignupMail(data, sessionKey)
        val to = To(List(Address(signupMail.signupForm.email)))
        SignupMailer.send(List(to), signupMail) match {
          case Success(x) => Ok(signupMail.toString)
          case Failure(e) => BadRequest(e.getMessage)
        }
      }
    )
  }

  def complete(code: String) = TODO
}
