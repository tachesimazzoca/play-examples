package models.form

import play.api.data.Forms._
import play.api.data._

case class AccountCreateForm(email: String, password: String)

object AccountCreateForm extends NormalizationSupport {

  import ConstraintHelper._

  override val nonBlankFields: Seq[String] =
    Seq("email", "password.main", "password.confirmation")

  private val form = Form(
    mapping(
      "email" -> text.verifying(email("AccountCreateForm.error.email")),
      "password" -> tuple(
        "main" -> text.verifying(password("AccountCreateForm.error.password")),
        "confirmation" -> text
      ).verifying(sameValue("AccountCreateForm.error.retypedPassword")),
      "uniqueEmail" -> default(boolean, true).verifying(
        passed("AccountCreateForm.error.uniqueEmail"))
    ) { (email, passwords, _) =>
      AccountCreateForm(email, passwords._1)
    } { a =>
      Some(a.email, (a.password, a.password), true)
    }
  )

  def defaultForm: Form[AccountCreateForm] = form

  def fromRequest(implicit request: play.api.mvc.Request[_]): Form[AccountCreateForm] =
    form.bindFromRequest(normalize(request))
}
