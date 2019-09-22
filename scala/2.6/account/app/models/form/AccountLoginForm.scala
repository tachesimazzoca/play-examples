package models.form

import play.api.data.Forms._
import play.api.data._

case class AccountLoginForm(
  email: String = "",
  password: String = "",
  returnTo: Option[String] = None,
  keepMeLoggedIn: Boolean = false,
  authorized: Boolean = true
)

object AccountLoginForm extends NormalizationSupport {

  import ConstraintHelper._

  override val nonBlankFields: Seq[String] = Seq("email", "password")

  private val form = Form(
    mapping(
      "email" -> text.verifying(nonBlank("AccountLoginForm.error.email")),
      "password" -> text.verifying(nonBlank("AccountLoginForm.error.password")),
      "returnTo" -> optional(text),
      "keepMeLoggedIn" -> default(boolean, false),
      "authorized" -> default(boolean, true)
        .verifying(passed("AccountLoginForm.error.authorized"))
    )(AccountLoginForm.apply)(AccountLoginForm.unapply)
  )

  def defaultForm: Form[AccountLoginForm] = form

  def fromRequest(implicit request: play.api.mvc.Request[_]): Form[AccountLoginForm] =
    form.bindFromRequest(normalize(request))

  def unbind(data: AccountLoginForm): Map[String, String] = form.mapping.unbind(data)
}
