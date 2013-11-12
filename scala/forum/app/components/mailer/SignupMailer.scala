package components.mailer

import settings._

import play.api.Play.current

import components.form.SignupForm

object SignupMailer extends TextMailer[SignupMail] {
  val settings: List[Setting] = loadSettings("SignupMailer")

  def render(param: SignupMail): String =
    views.txt._mails.signup(param).body
}

case class SignupMail(signupForm: SignupForm, sessionKey: String)
