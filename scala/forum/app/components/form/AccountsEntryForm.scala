package components.form

import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._

case class AccountsEntryForm(email: String, password: String)

object AccountsEntryForm {
  private val form = Form(
    mapping(
      "email" -> text.verifying(
        "AccountsEntryForm.error.email",
        _.matches( """^(.+)$""")
      ),
      "password" -> tuple(
        "main" -> text.verifying(
          "AccountsEntryForm.error.password",
          _.matches( """^[0-9a-zA-Z.+/=_-]{8,64}$""")
        ),
        "confirmation" -> text
      ).verifying(
          "AccountsEntryForm.error.passwords",
          passwords => passwords._1 == passwords._2
        ),
      "uniqueEmail" -> default(of[Boolean], true).verifying(
        "AccountsEntryForm.error.uniqueEmail",
        _ == true
      )
    ) { (email, passwords, _) =>
      AccountsEntryForm(email, passwords._1)
    } { a =>
      Some(a.email, ("", ""), true)
    }
  )

  def defaultForm: Form[AccountsEntryForm] = form
}
