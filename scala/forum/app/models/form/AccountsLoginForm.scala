package models.form

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._

case class AccountsLoginForm(email: String, password: String)

object AccountsLoginForm {
  private val form = Form(
    mapping(
      "email" -> text.verifying(
        "AccountsLoginForm.error.email",
        _.matches( """^(.+)$""")
      ),
      "password" -> text.verifying(
        "AccountsLoginForm.error.password",
        _.matches( """^(.+)$""")
      )
    ) { (email, password) =>
      AccountsLoginForm(email, password)
    } { a =>
      Some(a.email, a.password)
    }
  )

  def defaultForm: Form[AccountsLoginForm] = form
}
