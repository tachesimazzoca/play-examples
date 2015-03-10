package components.form

import play.api.data.Forms._
import play.api.data._

case class SignupForm(email: String, password: String)

object SignupForm {
  private val form = Form(
    mapping(
      "email" -> text.verifying(
        "SignupForm.error.email",
        _.matches( """^(.+)$""")
      ),
      "password" -> tuple(
        "main" -> text.verifying(
          "SignupForm.error.password",
          _.matches( """^[0-9a-zA-Z.+/=_-]{8,64}$""")
        ),
        "confirmation" -> text
      ).verifying(
          "SignupForm.error.passwords",
          passwords => passwords._1 == passwords._2
        )
    ) { (email, passwords) =>
      SignupForm(email, passwords._1)
    } { signup =>
      Some(signup.email, ("", ""))
    }
  )

  def apply(): Form[SignupForm] = form
}
